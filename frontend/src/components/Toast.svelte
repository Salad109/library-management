<script lang="ts">
    import {onMount} from 'svelte'

    const {message, onclose}: { message: string; onclose: () => void } = $props()

    onMount(() => {
        const timer = setTimeout(onclose, 5000)
        return () => clearTimeout(timer)
    })
</script>

<div class="toast toast-end">
    <div class="alert alert-success" role="status">{message}</div>
</div>
