import { useEffect, useState } from 'react'

// Componente principal de la aplicación.
function App() {

  // Guardamos el mensaje recibido desde el backend.
  const [mensaje, setMensaje] = useState('Conectando con el backend...')

  // Guardamos los posibles errores de conexión.
  const [error, setError] = useState('')

  // Solicitamos información al backend cuando se carga la aplicación.
  useEffect(() => {

    fetch('http://localhost:8080/api/prueba')
      .then((respuesta) => {

        // Comprobamos si la respuesta HTTP ha sido correcta.
        if (!respuesta.ok) {
          throw new Error('Error en la respuesta del servidor')
        }

        // Convertimos la respuesta JSON en un objeto JavaScript.
        return respuesta.json()

      })
      .then((datos) => {

        // Mostramos el mensaje recibido desde Spring Boot.
        setMensaje(datos.mensaje)

      })
      .catch((error) => {

        // Mostramos el error si falla la comunicación.
        console.error('Error de conexión:', error)
        setError('No se ha podido conectar con el backend.')

      })

  }, [])

  return (
    <main>
      <h1>hAIr - Gestor de citas</h1>

      <p>{mensaje}</p>

      {error && <p>{error}</p>}
    </main>
  )
}

export default App
