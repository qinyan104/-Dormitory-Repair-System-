export const getPageRecords = (data: any) => {
  if (Array.isArray(data)) {
    return data
  }

  return data?.records || data?.list || []
}
