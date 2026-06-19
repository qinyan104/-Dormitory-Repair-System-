declare module 'sockjs-client' {
  const SockJS: {
    new (url: string): WebSocket
  }
  export default SockJS
}
