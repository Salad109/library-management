<script lang="ts">
  import { checkout, CopyStatus, createCopies, getCopies, markLost, returnCopy } from './api'
  import type { CopyResponse, GetCopiesData, PagedModelCopyResponse } from './api'
  import Modal from './components/Modal.svelte'
  import Navbar from './components/Navbar.svelte'
  import Pagination from './components/Pagination.svelte'

  const searchTypes = [
    { value: 'title', label: 'Book title' },
    { value: 'isbn', label: 'ISBN' },
    { value: 'customerId', label: 'Customer ID' },
    { value: 'customerLastName', label: 'Customer last name' },
  ]

  const params = new URLSearchParams(location.search)
  const q = params.get('q')?.trim() ?? ''
  const searchType = params.get('searchType') ?? 'title'
  const pageNumber = Number(params.get('page') ?? 0)
  const searchLabel = searchTypes.find((type) => type.value === searchType)?.label

  let copies = $state<PagedModelCopyResponse>()
  let error = $state('')
  let notice = $state('')

  let dialog = $state<'checkout' | 'return' | 'add' | null>(null)
  let formError = $state('')
  let customerId = $state<number | null>(null)
  let copyId = $state<number | null>(null)
  let isbn = $state('')
  let quantity = $state<number | null>(1)

  function searchQuery() {
    const query: GetCopiesData['query'] = { page: pageNumber }
    if (q === '') return query
    if (searchType === 'isbn') query.isbn = q
    if (searchType === 'title') query.title = q
    if (searchType === 'customerId') query.customerId = Number(q)
    if (searchType === 'customerLastName') query.customerLastName = q
    return query
  }

  async function load() {
    if (searchType === 'customerId' && !Number.isInteger(Number(q))) {
      error = 'Customer ID must be a number'
      return
    }
    try {
      const response = await getCopies({ query: searchQuery() })
      copies = response.data
    } catch (e) {
      error = (e as Error).message
    }
  }

  function openDesk(action: 'checkout' | 'return') {
    formError = ''
    customerId = null
    copyId = null
    dialog = action
  }

  function openAdd() {
    formError = ''
    isbn = ''
    quantity = 1
    dialog = 'add'
  }

  // Runs a dialog's write, then closes the dialog and refetches; a failure stays in the dialog
  async function submit(event: SubmitEvent, write: () => Promise<unknown>, successMessage: string) {
    event.preventDefault()
    formError = ''
    notice = ''
    try {
      await write()
      notice = successMessage
      dialog = null
      await load()
    } catch (e) {
      formError = (e as Error).message
    }
  }

  function submitDesk(event: SubmitEvent) {
    const body = { copyId: copyId as number, customerId: customerId as number }
    if (dialog === 'checkout') {
      submit(event, () => checkout({ body: body }), 'Checkout completed successfully')
    } else {
      submit(event, () => returnCopy({ body: body }), 'Return completed successfully')
    }
  }

  function submitAdd(event: SubmitEvent) {
    const body = { bookIsbn: isbn, quantity: quantity as number }
    submit(event, () => createCopies({ body: body }), 'Copies added successfully')
  }

  async function markCopyLost(copy: CopyResponse) {
    if (!confirm(`Mark copy ${copy.id} of "${copy.book.title}" as lost? This can't be undone.`)) return
    error = ''
    notice = ''
    try {
      await markLost({ body: { copyId: copy.id } })
      notice = 'Copy marked as lost'
      await load()
    } catch (e) {
      error = (e as Error).message
    }
  }

  load()
</script>

<Navbar />

<main class="container mx-auto px-4 pb-8">
  <h1 class="mb-4 text-3xl font-bold">Copies</h1>

  <div class="mb-4 flex flex-wrap justify-between gap-2">
    <form class="join">
      <select class="select join-item w-48" name="searchType" aria-label="Search by">
        {#each searchTypes as type (type.value)}
          <option value={type.value} selected={type.value === searchType}>{type.label}</option>
        {/each}
      </select>
      <input class="input join-item w-64" name="q" value={q} placeholder="Enter search term..." autocomplete="off" />
      <button class="btn btn-primary join-item" type="submit">Search</button>
      <a class="btn join-item" href="?">Clear</a>
    </form>
    <div>
      <button class="btn btn-success" onclick={() => openDesk('checkout')}>Checkout</button>
      <button class="btn btn-warning" onclick={() => openDesk('return')}>Return</button>
      <button class="btn btn-info" onclick={openAdd}>Add Copies</button>
    </div>
  </div>

  {#if error}
    <div class="alert alert-error mb-4">{error}</div>
  {/if}
  {#if notice}
    <div class="alert alert-success mb-4">{notice}</div>
  {/if}

  {#if copies}
    <p class="mb-2 opacity-70">{q ? `Showing results for ${searchLabel}: "${q}"` : 'Showing all copies'}</p>

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
            <td>{copy.status}</td>
            <td>
              <button class="btn btn-sm btn-error" onclick={() => markCopyLost(copy)} disabled={copy.status === CopyStatus.LOST}>
                Mark Lost
              </button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>

    <Pagination page={copies.page} />
  {/if}
</main>

{#if dialog === 'checkout' || dialog === 'return'}
  <Modal title={dialog === 'checkout' ? 'Checkout Copy' : 'Return Copy'} onclose={() => (dialog = null)}>
    <form onsubmit={submitDesk}>
      {#if formError}
        <div class="alert alert-error mb-4">{formError}</div>
      {/if}
      <div class="mb-3">
        <label class="label mb-1" for="customerId">Customer ID</label>
        <input class="input w-full" id="customerId" type="number" bind:value={customerId} required />
      </div>
      <div class="mb-3">
        <label class="label mb-1" for="copyId">Copy ID</label>
        <input class="input w-full" id="copyId" type="number" bind:value={copyId} required />
      </div>
      <div class="modal-action">
        <button class="btn" type="button" onclick={() => (dialog = null)}>Cancel</button>
        <button class="btn btn-primary" type="submit">{dialog === 'checkout' ? 'Checkout' : 'Return'}</button>
      </div>
    </form>
  </Modal>
{:else if dialog === 'add'}
  <Modal title="Add Copies" onclose={() => (dialog = null)}>
    <form onsubmit={submitAdd}>
      {#if formError}
        <div class="alert alert-error mb-4">{formError}</div>
      {/if}
      <div class="mb-3">
        <label class="label mb-1" for="isbn">Book ISBN</label>
        <input class="input w-full" id="isbn" bind:value={isbn} required />
      </div>
      <div class="mb-3">
        <label class="label mb-1" for="quantity">Quantity</label>
        <input class="input w-full" id="quantity" type="number" min="1" max="100" bind:value={quantity} required />
      </div>
      <div class="modal-action">
        <button class="btn" type="button" onclick={() => (dialog = null)}>Cancel</button>
        <button class="btn btn-primary" type="submit">Add</button>
      </div>
    </form>
  </Modal>
{/if}
