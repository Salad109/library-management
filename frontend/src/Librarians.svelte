<script lang="ts">
    import {createLibrarian} from './api'
    import FieldError from './components/FieldError.svelte'
    import Navbar from './components/Navbar.svelte'

    let email = $state('')
    let password = $state('')
    let error = $state<Error>()
    let notice = $state('')

    async function submit(event: SubmitEvent) {
        event.preventDefault()
        error = undefined
        notice = ''
        try {
            const response = await createLibrarian({body: {email: email, password: password}})
            notice = `Librarian added: ${response.data.email}`
            email = ''
            password = ''
        } catch (e) {
            error = e as Error
        }
    }
</script>

<Navbar/>

<main class="container mx-auto max-w-md px-4 pb-8">
    <h1 class="mb-4 text-3xl font-bold">Add Librarian</h1>

    {#if error}
        <div class="alert alert-error mb-4">{error.message}</div>
    {/if}
    {#if notice}
        <div class="alert alert-success mb-4">{notice}</div>
    {/if}

    <form onsubmit={submit}>
        <div class="mb-3">
            <label class="label mb-1" for="email">Email</label>
            <input autocomplete="off" bind:value={email} class="input w-full" id="email" required type="email"/>
            <FieldError error={error} field="email"/>
        </div>
        <div class="mb-3">
            <label class="label mb-1" for="password">Password</label>
            <input autocomplete="new-password" bind:value={password} class="input w-full" id="password" required
                   type="password"/>
            <FieldError error={error} field="password"/>
        </div>
        <button class="btn btn-primary" type="submit">Add Librarian</button>
    </form>
</main>
