<script lang="ts">
  import { logout, Role, whoAmI } from '../api'

  const links = [
    { href: '/books.html', label: 'Books' },
    { href: '/admin/copies/browse', label: 'Copies' },
    { href: '/admin/customers/browse', label: 'Customers' },
    { href: '/admin/librarians/add', label: 'Add Librarian' },
  ]

  let username = $state('')

  whoAmI().then((response) => {
    const me = response.data
    if (me.role === Role.ROLE_LIBRARIAN) {
      username = me.username ?? ''
    } else {
      location.href = '/login.html'
    }
  })

  async function signOut() {
    await logout()
    location.href = '/login.html'
  }
</script>

<nav class="navbar bg-base-200 mb-6 px-4">
  <a class="btn btn-ghost text-lg" href="/admin">Library Admin</a>
  <ul class="menu menu-horizontal flex-1">
    {#each links as link (link.href)}
      <li><a class:menu-active={location.pathname === link.href} href={link.href}>{link.label}</a></li>
    {/each}
  </ul>
  <span class="mr-4">{username}</span>
  <button class="btn btn-outline btn-sm" onclick={signOut}>Logout</button>
</nav>
