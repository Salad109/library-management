<script lang="ts">
  import { login, Role } from './api'

  let username = $state('')
  let password = $state('')
  let error = $state('')
  let loggedInAs = $state('')

  async function submit(event: SubmitEvent) {
    event.preventDefault()
    error = ''
    try {
      const response = await login({ body: { username: username, password: password } })
      const me = response.data
      if (me.role === Role.ROLE_LIBRARIAN) {
        location.href = '/admin'
      } else {
        loggedInAs = me.username
      }
    } catch (e) {
      error = (e as Error).message
    }
  }
</script>

<main class="mx-auto mt-16 max-w-md px-4">
  <h1 class="mb-4 text-3xl font-bold">Login</h1>

  {#if loggedInAs}
    <div class="alert alert-success">Logged in as {loggedInAs}</div>
  {:else}
    {#if error}
      <div class="alert alert-error mb-4">{error}</div>
    {/if}

    <form onsubmit={submit}>
      <div class="mb-3">
        <label class="label mb-1" for="username">Username</label>
        <input class="input w-full" id="username" bind:value={username} required autocomplete="username" />
      </div>

      <div class="mb-3">
        <label class="label mb-1" for="password">Password</label>
        <input class="input w-full" id="password" type="password" bind:value={password} required autocomplete="current-password" />
      </div>

      <button class="btn btn-primary" type="submit">Login</button>
    </form>
  {/if}
</main>
