<script lang="ts">
  import { createCustomer, generateActivationCode, getCustomers, updateCustomer } from './api'
  import type { ActivationCodeResponse, CustomerResponse, PagedModelCustomerResponse } from './api'
  import Modal from './components/Modal.svelte'
  import Navbar from './components/Navbar.svelte'
  import Pagination from './components/Pagination.svelte'

  const params = new URLSearchParams(location.search)
  const q = params.get('q')?.trim() ?? ''
  const pageNumber = Number(params.get('page') ?? 0)

  let customers = $state<PagedModelCustomerResponse>()
  let error = $state('')
  let addedCustomer = $state<CustomerResponse>()
  let activationCode = $state<ActivationCodeResponse>()
  let activationCustomer = $state<CustomerResponse>()

  let formOpen = $state(false)
  let editing = $state(false)
  let formError = $state('')
  let id = $state<number | null>(null)
  let firstName = $state('')
  let lastName = $state('')
  // Undefined for a customer without an account
  let email = $state<string | undefined>()

  async function load() {
    try {
      const response = q
        ? await getCustomers({ query: { lastName: q, page: pageNumber } })
        : await getCustomers({ query: { page: pageNumber } })
      customers = response.data
    } catch (e) {
      error = (e as Error).message
    }
  }

  function fullName(customer: CustomerResponse) {
    return `${customer.firstName} ${customer.lastName}`
  }

  function openAdd() {
    editing = false
    formError = ''
    id = null
    firstName = ''
    lastName = ''
    email = undefined
    formOpen = true
  }

  function openEdit(customer: CustomerResponse) {
    editing = true
    formError = ''
    id = customer.id
    firstName = customer.firstName
    lastName = customer.lastName
    email = customer.email
    formOpen = true
  }

  async function save(event: SubmitEvent) {
    event.preventDefault()
    formError = ''
    addedCustomer = undefined
    try {
      if (editing) {
        await updateCustomer({
          path: { id: id as number },
          body: { firstName: firstName, lastName: lastName, email: email },
        })
      } else {
        const response = await createCustomer({ body: { firstName: firstName, lastName: lastName } })
        addedCustomer = response.data
      }
      formOpen = false
      await load()
    } catch (e) {
      formError = (e as Error).message
    }
  }

  async function generateCode(customer: CustomerResponse) {
    error = ''
    activationCode = undefined
    try {
      const response = await generateActivationCode({ path: { id: customer.id } })
      activationCode = response.data
      activationCustomer = customer
    } catch (e) {
      error = (e as Error).message
    }
  }

  load()
</script>

<Navbar />

<main class="container mx-auto px-4 pb-8">
  <h1 class="mb-4 text-3xl font-bold">Customers</h1>

  <div class="mb-4 flex justify-between">
    <form class="join">
      <input class="input join-item w-80" name="q" value={q} placeholder="Customer's last name..." autocomplete="off" />
      <button class="btn btn-primary join-item" type="submit">Search</button>
      <a class="btn join-item" href="?">Clear</a>
    </form>
    <button class="btn btn-success" onclick={openAdd}>Add Customer</button>
  </div>

  {#if error}
    <div class="alert alert-error mb-4">{error}</div>
  {/if}
  {#if addedCustomer}
    {@const customer = addedCustomer}
    <div class="alert alert-success mb-4">
      <span>Added {fullName(customer)}, customer ID {customer.id}</span>
      <button class="btn btn-sm" onclick={() => generateCode(customer)}>Generate activation code</button>
    </div>
  {/if}
  {#if activationCode && activationCustomer}
    <div class="alert alert-success mb-4 block">
      <p>Activation code for {fullName(activationCustomer)}: <code class="text-2xl">{activationCode.activationCode}</code></p>
      <p>Valid until {new Date(activationCode.expiresAt).toLocaleString()}. Generating a new code replaces this one.</p>
    </div>
  {/if}

  {#if customers}
    <p class="mb-2 opacity-70">{q ? `Showing results for: "${q}"` : 'Showing all customers'}</p>

    <table class="table table-zebra mb-4">
      <thead>
        <tr>
          <th>ID</th>
          <th>Full name</th>
          <th>Email</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {#each customers.content as customer (customer.id)}
          <tr>
            <td>{customer.id}</td>
            <td>{fullName(customer)}</td>
            <td>{customer.email ?? '-'}</td>
            <td>
              <button class="btn btn-sm btn-warning" onclick={() => openEdit(customer)}>Edit</button>
              {#if !customer.email}
                <button class="btn btn-sm btn-success" onclick={() => generateCode(customer)}>Activation code</button>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>

    <Pagination page={customers.page} />
  {/if}
</main>

{#if formOpen}
  <Modal title={editing ? 'Edit Customer' : 'Add Customer'} onclose={() => (formOpen = false)}>
    <form onsubmit={save}>
      {#if formError}
        <div class="alert alert-error mb-4">{formError}</div>
      {/if}
      <div class="mb-3">
        <label class="label mb-1" for="firstName">First name</label>
        <input class="input w-full" id="firstName" bind:value={firstName} required />
      </div>
      <div class="mb-3">
        <label class="label mb-1" for="lastName">Last name</label>
        <input class="input w-full" id="lastName" bind:value={lastName} required />
      </div>
      {#if email !== undefined}
        <div class="mb-3">
          <label class="label mb-1" for="email">Email</label>
          <input class="input w-full" id="email" bind:value={email} required />
        </div>
      {/if}
      <div class="modal-action">
        <button class="btn" type="button" onclick={() => (formOpen = false)}>Cancel</button>
        <button class="btn btn-primary" type="submit">Save</button>
      </div>
    </form>
  </Modal>
{/if}
