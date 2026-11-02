<script lang="ts">
  import { createBook, deleteBook, getAllBooks, searchBooks, updateBook } from './api'
  import type { BookResponse, PagedModelBookResponse } from './api'
  import Modal from './components/Modal.svelte'
  import Navbar from './components/Navbar.svelte'
  import Pagination from './components/Pagination.svelte'

  const params = new URLSearchParams(location.search)
  const q = params.get('q')?.trim() ?? ''
  const pageNumber = Number(params.get('page') ?? 0)

  let books = $state<PagedModelBookResponse>()
  let error = $state('')
  let notice = $state('')

  let formOpen = $state(false)
  let editing = $state(false)
  let formError = $state('')
  let isbn = $state('')
  let title = $state('')
  let year = $state<number | null>(null)
  let authors = $state('')

  async function load() {
    try {
      const response = q
        ? await searchBooks({ query: { q: q, page: pageNumber } })
        : await getAllBooks({ query: { page: pageNumber } })
      books = response.data
    } catch (e) {
      error = (e as Error).message
    }
  }

  function authorNames(book: BookResponse) {
    return book.authors.map((author) => author.name).join(', ')
  }

  function openAdd() {
    editing = false
    formError = ''
    isbn = ''
    title = ''
    year = null
    authors = ''
    formOpen = true
  }

  function openEdit(book: BookResponse) {
    editing = true
    formError = ''
    isbn = book.isbn
    title = book.title
    year = book.publicationYear ?? null
    authors = authorNames(book)
    formOpen = true
  }

  async function save(event: SubmitEvent) {
    event.preventDefault()
    formError = ''
    notice = ''
    const publicationYear = year ?? undefined
    const names = authors.split(',').map((name) => name.trim())
    try {
      if (editing) {
        await updateBook({
          path: { isbn: isbn },
          body: { title: title, publicationYear: publicationYear, authorNames: names },
        })
      } else {
        await createBook({
          body: { isbn: isbn, title: title, publicationYear: publicationYear, authorNames: names },
        })
        notice = `Added "${title}"`
      }
      formOpen = false
      await load()
    } catch (e) {
      formError = (e as Error).message
    }
  }

  async function remove(book: BookResponse) {
    if (!confirm(`Are you sure you want to delete "${book.title}"?`)) return
    error = ''
    notice = ''
    try {
      await deleteBook({ path: { isbn: book.isbn } })
      await load()
    } catch (e) {
      error = (e as Error).message
    }
  }

  load()
</script>

<Navbar />

<main class="container mx-auto px-4 pb-8">
  <h1 class="mb-4 text-3xl font-bold">Books</h1>

  <div class="mb-4 flex justify-between">
    <form class="join">
      <input class="input join-item w-80" name="q" value={q} placeholder="Title or author..." autocomplete="off" />
      <button class="btn btn-primary join-item" type="submit">Search</button>
      <a class="btn join-item" href="?">Clear</a>
    </form>
    <button class="btn btn-success" onclick={openAdd}>Add Book</button>
  </div>

  {#if error}
    <div class="alert alert-error mb-4">{error}</div>
  {/if}
  {#if notice}
    <div class="alert alert-success mb-4">{notice}</div>
  {/if}

  {#if books}
    <p class="mb-2 opacity-70">{q ? `Showing results for: "${q}"` : 'Showing all books'}</p>

    <table class="table table-zebra mb-4">
      <thead>
        <tr>
          <th>ISBN</th>
          <th>Title</th>
          <th>Year</th>
          <th>Authors</th>
          <th>Available copies</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {#each books.content as book (book.isbn)}
          <tr>
            <td>{book.isbn}</td>
            <td>{book.title}</td>
            <td>{book.publicationYear}</td>
            <td>{authorNames(book)}</td>
            <td>{book.availableCopies}</td>
            <td>
              <a class="btn btn-sm btn-primary" href="/api/books/{book.isbn}" target="_blank">View</a>
              <button class="btn btn-sm btn-warning" onclick={() => openEdit(book)}>Edit</button>
              <button class="btn btn-sm btn-error" onclick={() => remove(book)}>Delete</button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>

    <Pagination page={books.page} />
  {/if}
</main>

{#if formOpen}
  <Modal title={editing ? 'Edit Book' : 'Add Book'} onclose={() => (formOpen = false)}>
    <form onsubmit={save}>
      {#if formError}
        <div class="alert alert-error mb-4">{formError}</div>
      {/if}
      <div class="mb-3">
        <label class="label mb-1" for="isbn">ISBN</label>
        <input class="input w-full" id="isbn" bind:value={isbn} disabled={editing} required />
      </div>
      <div class="mb-3">
        <label class="label mb-1" for="title">Title</label>
        <input class="input w-full" id="title" bind:value={title} required />
      </div>
      <div class="mb-3">
        <label class="label mb-1" for="year">Publication Year</label>
        <input class="input w-full" id="year" type="number" bind:value={year} />
      </div>
      <div class="mb-3">
        <label class="label mb-1" for="authors">Authors (comma-separated)</label>
        <input class="input w-full" id="authors" bind:value={authors} required />
      </div>
      <div class="modal-action">
        <button class="btn" type="button" onclick={() => (formOpen = false)}>Cancel</button>
        <button class="btn btn-primary" type="submit">Save</button>
      </div>
    </form>
  </Modal>
{/if}
