export const REPAIR_STATUS_MAP: Record<number, { label: string, color: string }> = {
  1: { label: '待受理', color: 'var(--mc-muted)' },
  2: { label: '已派单', color: 'var(--mc-signal-light)' },
  3: { label: '维修中', color: 'var(--mc-signal)' },
  4: { label: '待确认', color: 'var(--mc-warning)' },
  5: { label: '已完成', color: 'var(--mc-success)' },
  6: { label: '已取消', color: 'var(--mc-muted-soft)' }
}
