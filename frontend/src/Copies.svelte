<script lang="ts">
    import type {CopyResponse, GetCopiesData, PagedModelCopyResponse} from './api'
    import {checkout, CopyStatus, createCopies, getCopies, markLost, renewLoan, returnCopy} from './api'
    import FieldError from './components/FieldError.svelte'
    import Modal from './components/Modal.svelte'
    import Navbar from './components/Navbar.svelte'
    import Pagination from './components/Pagination.svelte'
    import Toast from './components/Toast.svelte'

    const searchTypes = [
        {value: 'title', label: 'Book title'},
        {value: 'isbn', label: 'ISBN'},
        {value: 'customerId', label: 'Customer ID'},
        {value: 'customerLastName', label: 'Customer last name'},
    ]

    const params = new URLSearchParams(location.search)
    const q = params.get('q')?.trim() ?? ''
    const searchType = params.get('searchType') ?? 'title'
    const pageNumber = Number(params.get('page') ?? 0)
    const overdue = params.get('overdue') === 'true'
    const overdueText = overdue ? 'overdue ' : ''
    const searchLabel = searchTypes.find((type) => type.value === searchType)?.label

    let copies = $state<PagedModelCopyResponse>()
    let error = $state('')
    let notice = $state('')

    type Action = 'checkout' | 'return' | 'renew' | 'add' | 'lost'

    // The top bar's buttons
    type ActionEntry = { action: Action; label: string; buttonClass: string; title: string; submitLabel: string }
    const actions: ActionEntry[] = [
        {action: 'checkout', label: 'Checkout', buttonClass: 'btn-success', title: 'Checkout Copy',
            submitLabel: 'Checkout'},
        {action: 'return', label: 'Return', buttonClass: 'btn-warning', title: 'Return Copy', submitLabel: 'Return'},
        {action: 'renew', label: 'Renew', buttonClass: 'btn-secondary', title: 'Renew Loan', submitLabel: 'Renew'},
        {action: 'add', label: 'Add Copies', buttonClass: 'btn-info', title: 'Add Copies', submitLabel: 'Add'},
        {action: 'lost', label: 'Mark Lost', buttonClass: 'btn-error', title: 'Mark Copy Lost',
            submitLabel: 'Mark Lost'},
    ]
    const rowActions = actions.filter((entry) => entry.action !== 'add')

    // The clicked button's entry; null while no dialog is open
    let dialog = $state<ActionEntry | null>(null)
    // The copy whose row button opened the dialog, null when a top-bar button did
    let rowCopy = $state<CopyResponse | null>(null)
    let formError = $state<Error>()
    let customerId = $state<number | null>(null)
    let copyId = $state<number | null>(null)
    let isbn = $state('')
    let quantity = $state<number | null>(1)

    function searchQuery() {
        const query: GetCopiesData['query'] = {page: pageNumber}
        if (overdue) query.overdue = true
        if (q === '') return query
        if (searchType === 'isbn') query.isbn = q
        if (searchType === 'title') query.title = q
        if (searchType === 'customerId') query.customerId = Number(q)
        if (searchType === 'customerLastName') query.customerLastName = q
        return query
    }

    function isOverdue(copy: CopyResponse) {
        return copy.status === CopyStatus.BORROWED && copy.dueAt !== undefined && new Date(copy.dueAt) < new Date()
    }

    // Whole days past the due date
    function daysOverdue(dueAt: string) {
        return Math.floor((Date.now() - new Date(dueAt).getTime()) / (24 * 60 * 60 * 1000))
    }

    async function load() {
        if (searchType === 'customerId' && !Number.isInteger(Number(q))) {
            error = 'Customer ID must be a number'
            return
        }
        try {
            const response = await getCopies({query: searchQuery()})
            copies = response.data
        } catch (e) {
            error = (e as Error).message
        }
    }

    // Whether a row's button applies to that copy's status
    function allowed(action: Action, copy: CopyResponse) {
        if (action === 'checkout') return copy.status === CopyStatus.AVAILABLE || copy.status === CopyStatus.RESERVED
        if (action === 'return' || action === 'renew') return copy.status === CopyStatus.BORROWED
        return copy.status !== CopyStatus.LOST
    }

    function openDialog(entry: ActionEntry, copy: CopyResponse | null) {
        formError = undefined
        rowCopy = copy
        copyId = copy ? copy.id : null
        customerId = copy && copy.customer ? copy.customer.id : null
        isbn = ''
        quantity = 1
        dialog = entry
    }

    // Checkout, return and renew need a customer, unless the row's copy already has one
    function asksCustomer() {
        if (dialog === null || dialog.action === 'add' || dialog.action === 'lost') return false
        return rowCopy === null || rowCopy.customer === undefined
    }

    // Runs a dialog's write, then closes the dialog and refetches; a failure stays in the dialog
    async function submit(event: SubmitEvent, write: () => Promise<unknown>, successMessage: string) {
        event.preventDefault()
        formError = undefined
        notice = ''
        try {
            await write()
            notice = successMessage
            dialog = null
            await load()
        } catch (e) {
            formError = e as Error
        }
    }

    function submitDialog(event: SubmitEvent) {
        const action = dialog?.action
        const deskBody = {copyId: copyId as number, customerId: customerId as number}
        if (action === 'checkout') {
            submit(event, () => checkout({body: deskBody}), 'Checkout completed successfully')
        } else if (action === 'return') {
            submit(event, () => returnCopy({body: deskBody}), 'Return completed successfully')
        } else if (action === 'renew') {
            submit(event, () => renewLoan({body: deskBody}), 'Loan renewed successfully')
        } else if (action === 'add') {
            const addBody = {bookIsbn: isbn, quantity: quantity as number}
            submit(event, () => createCopies({body: addBody}), 'Copies added successfully')
        } else {
            submit(event, () => markLost({body: {copyId: copyId as number}}), 'Copy marked as lost')
        }
    }

    load()
</script>

<Navbar/>

<main class="container mx-auto px-4 pb-8">
    <h1 class="mb-4 text-3xl font-bold">Copies</h1>

    <div class="mb-4 flex flex-wrap justify-between gap-2">
        <form class="flex flex-wrap items-center gap-4">
            <div class="join">
                <select aria-label="Search by" class="select join-item w-48" name="searchType">
                    {#each searchTypes as type (type.value)}
                        <option value={type.value} selected={type.value === searchType}>{type.label}</option>
                    {/each}
                </select>
                <input autocomplete="off" class="input join-item w-64" name="q" placeholder="Enter search term..."
                       value={q}/>
                <button class="btn btn-primary join-item" type="submit">Search</button>
                <a class="btn join-item" href="?">Clear</a>
            </div>
            <label class="label">
                <input class="checkbox" name="overdue" type="checkbox" value="true" checked={overdue}/>
                Overdue only
            </label>
        </form>
        <div class="flex flex-wrap gap-1">
            {#each actions as entry (entry.action)}
                <button class="btn {entry.buttonClass}" onclick={() => openDialog(entry, null)}>
                    {entry.label}
                </button>
            {/each}
        </div>
    </div>

    {#if error}
        <div class="alert alert-error mb-4">{error}</div>
    {/if}
    {#if notice}
        <Toast message={notice} onclose={() => (notice = '')}/>
    {/if}

    {#if copies}
        <p class="mb-2 opacity-70">
            {q ? `Showing ${overdueText}results for ${searchLabel}: "${q}"` : `Showing all ${overdueText}copies`}
        </p>

        <table class="table table-zebra mb-4">
            <thead>
            <tr>
                <th>ID</th>
                <th>Book title</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {#each copies.content as copy (copy.id)}
                <tr>
                    <td>{copy.id}</td>
                    <td>{copy.book.title}</td>
                    <td>
                        {#if copy.customer}
                            {copy.customer.firstName} {copy.customer.lastName} <strong>(ID: {copy.customer.id})</strong>
                        {:else}
                            -
                        {/if}
                    </td>
                    <td>
                        {copy.status}
                        {#if copy.dueAt && isOverdue(copy)}
                            <div class="text-xs text-error">
                                overdue since {new Date(copy.dueAt).toLocaleDateString()} ({daysOverdue(copy.dueAt)} days)
                            </div>
                        {:else if copy.dueAt}
                            <div class="text-xs opacity-70">until {new Date(copy.dueAt).toLocaleString()}</div>
                        {/if}
                        {#if copy.renewals > 0}
                            <div class="text-xs opacity-70">renewed {copy.renewals}×</div>
                        {/if}
                    </td>
                    <td>
                        <div class="flex flex-wrap gap-1">
                            {#each rowActions as entry (entry.action)}
                                <button class="btn btn-sm {entry.buttonClass}"
                                        onclick={() => openDialog(entry, copy)}
                                        disabled={!allowed(entry.action, copy)}>
                                    {entry.label}
                                </button>
                            {/each}
                        </div>
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>

        <Pagination page={copies.page}/>
    {/if}
</main>

{#if dialog !== null}
    <Modal title={dialog.title} onclose={() => (dialog = null)}>
        <form onsubmit={submitDialog}>
            {#if formError}
                <div class="alert alert-error mb-4">{formError.message}</div>
            {/if}
            {#if rowCopy}
                <p class="mb-3">Copy {rowCopy.id} of "{rowCopy.book.title}"</p>
                {#if rowCopy.customer}
                    <p class="mb-3">
                        Customer: {rowCopy.customer.firstName} {rowCopy.customer.lastName} (ID: {rowCopy.customer.id})
                    </p>
                {/if}
            {/if}

            {#if asksCustomer()}
                <div class="mb-3">
                    <label class="label mb-1" for="customerId">Customer ID</label>
                    <input class="input w-full" id="customerId" type="number" bind:value={customerId} required/>
                    <FieldError error={formError} field="customerId"/>
                </div>
            {/if}
            {#if dialog.action !== 'add' && rowCopy === null}
                <div class="mb-3">
                    <label class="label mb-1" for="copyId">Copy ID</label>
                    <input class="input w-full" id="copyId" type="number" bind:value={copyId} required/>
                    <FieldError error={formError} field="copyId"/>
                </div>
            {/if}
            {#if dialog.action === 'add'}
                <div class="mb-3">
                    <label class="label mb-1" for="isbn">Book ISBN</label>
                    <input class="input w-full" id="isbn" bind:value={isbn} required/>
                    <FieldError error={formError} field="bookIsbn"/>
                </div>
                <div class="mb-3">
                    <label class="label mb-1" for="quantity">Quantity</label>
                    <input class="input w-full" id="quantity" type="number" min="1" max="100" bind:value={quantity}
                           required/>
                    <FieldError error={formError} field="quantity"/>
                </div>
            {/if}
            {#if dialog.action === 'lost'}
                <p class="mb-3">This can't be undone.</p>
            {/if}

            <div class="modal-action">
                <button class="btn" type="button" onclick={() => (dialog = null)}>Cancel</button>
                <button class="btn btn-primary" type="submit">{dialog.submitLabel}</button>
            </div>
        </form>
    </Modal>
{/if}
