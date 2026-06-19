export type RepairCategoryOption = {
  id: number | string
  categoryName?: string
}

export type RepairUserInfo = {
  dormitoryBuilding?: string
  roomNo?: string
}

const splitImageUrls = (imageUrl?: string) => {
  if (!imageUrl) {
    return []
  }

  return imageUrl
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

export const normalizeRepairOrder = (
  order: any,
  categories: RepairCategoryOption[] = [],
  currentUser?: RepairUserInfo
) => {
  const categoryName =
    order.categoryName ??
    categories.find((item) => String(item.id) === String(order.categoryId))?.categoryName ??
    ''

  const images = Array.isArray(order.images) ? order.images : splitImageUrls(order.imageUrl)

  return {
    ...order,
    categoryName,
    content: order.content ?? order.description ?? '',
    description: order.description ?? order.content ?? '',
    images,
    imageUrl: order.imageUrl ?? images.join(','),
    processRemark: order.processRemark ?? order.remark ?? '',
    dormitoryBuilding: order.dormitoryBuilding ?? currentUser?.dormitoryBuilding ?? '',
    roomNo: order.roomNo ?? currentUser?.roomNo ?? ''
  }
}
