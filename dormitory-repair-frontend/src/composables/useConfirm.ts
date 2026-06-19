import { reactive } from 'vue'

interface ConfirmState {
  visible: boolean
  title: string
  message: string
  resolve: ((value: boolean) => void) | null
}

const state = reactive<ConfirmState>({
  visible: false,
  title: '',
  message: '',
  resolve: null
})

export function useConfirm() {
  const confirm = (message: string, title = '确认操作'): Promise<boolean> => {
    return new Promise((resolve) => {
      state.visible = true
      state.title = title
      state.message = message
      state.resolve = resolve
    })
  }

  const handleConfirm = () => {
    state.resolve?.(true)
    state.visible = false
    state.resolve = null
  }

  const handleCancel = () => {
    state.resolve?.(false)
    state.visible = false
    state.resolve = null
  }

  return { confirmState: state, confirm, handleConfirm, handleCancel }
}
