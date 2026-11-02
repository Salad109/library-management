<script lang="ts">
  import type { PageMetadata } from '../api'

  const { page }: { page: PageMetadata } = $props()

  const last = $derived(page.totalPages - 1)
  const onFirst = $derived(page.number === 0)
  const onLast = $derived(page.number >= last)
  const firstItem = $derived(page.number * page.size + 1)
  const lastItem = $derived(Math.min(firstItem + page.size - 1, page.totalElements))

  function href(number: number) {
    const url = new URL(location.href)
    url.searchParams.set('page', String(number))
    return url.search
  }
</script>

{#snippet link(number: number, label: string, disabled: boolean)}
  <a class="join-item btn btn-sm" class:btn-disabled={disabled} href={href(number)} tabindex={disabled ? -1 : undefined}>{label}</a>
{/snippet}

{#if page.totalPages > 1 || page.number > 0}
  <nav class="flex justify-center" aria-label="Pagination">
    <div class="join">
      {@render link(0, '« First', onFirst)}
      {@render link(page.number - 1, '‹ Previous', onFirst)}
      <span class="join-item btn btn-sm btn-disabled">Page {page.number + 1} of {page.totalPages}</span>
      {@render link(page.number + 1, 'Next ›', onLast)}
      {@render link(last, 'Last »', onLast)}
    </div>
  </nav>
{/if}

{#if firstItem <= lastItem}
  <div class="mt-4 text-center opacity-70">Showing {firstItem}-{lastItem} of {page.totalElements}</div>
{/if}
