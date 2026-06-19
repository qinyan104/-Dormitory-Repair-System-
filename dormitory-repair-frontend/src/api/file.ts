import http from './http'

export const uploadFileApi = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
