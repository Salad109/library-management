<script lang="ts">
  import { ApiError } from '../api'

  const { error, field }: { error: Error | undefined; field: string } = $props()

  const messages = $derived(error instanceof ApiError ? error.fieldErrors(field) : [])
</script>

{#each messages as message}
  <p class="text-error mt-1 text-sm">{message}</p>
{/each}
