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
      const { data: me } = await login({ body: { username, password } })
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

<main class="container">
  <div class="row justify-content-center">
    <div class="col-8">
      <h1>Login</h1>

      {#if loggedInAs}
        <div class="alert alert-success">Logged in as {loggedInAs}</div>
      {:else}
        {#if error}
          <div class="alert alert-danger">{error}</div>
        {/if}

        <form onsubmit={submit}>
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input class="form-control" id="username" bind:value={username} required autocomplete="username" />
          </div>

          <div class="mb-3">
            <label class="form-label" for="password">Password</label>
            <input class="form-control" id="password" type="password" bind:value={password} required autocomplete="current-password" />
          </div>

          <button class="btn btn-primary" type="submit">Login</button>
        </form>
      {/if}
    </div>
  </div>
</main>
