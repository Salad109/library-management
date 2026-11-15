<script lang="ts">
    import type {Snippet} from 'svelte'

    const {title, onclose, children}: { title: string; onclose: () => void; children: Snippet } = $props()
</script>

<dialog {@attach (dialog) => dialog.showModal()} class="modal" onclose={onclose}>
    <div class="modal-box">
        <h3 class="mb-4 text-lg font-bold">{title}</h3>
        {@render children()}
    </div>
</dialog>
