# Contrato de integración: Citas y bloqueos

**Estado:** propuesta simplificada conforme a la decisión del proyecto de usar transacciones JDBC normales de Spring.  
**Alcance:** integración de Persona 2 (Citas) con Persona 3 (Disponibilidad y bloqueos). No implementa ni requiere un sistema de locks, leases o exclusión mutua.

## 1. Responsabilidades

| Responsabilidad | Propietario | Obligación |
|---|---|---|
| Crear y eliminar bloqueos de disponibilidad | Persona 3 | Gestionar las entidades y tablas que son responsabilidad de su módulo. |
| Abrir la transacción que engloba el bloqueo y las citas afectadas | Persona 3 | Ejecutar ambas operaciones bajo la misma transacción Spring. |
| Invocar la cancelación por bloqueo | Persona 3 | Llamar al método público del bean `CitaService` inyectado por Spring, no crear una instancia con `new` ni evitar el proxy. |
| Buscar y cancelar citas afectadas | Persona 2 | Consultar y cancelar únicamente citas `CONFIRMADA` que solapen el intervalo recibido. |
| Calcular disponibilidad y gestionar sus reglas | Persona 3 | Mantener la fuente canónica de horarios, apertura y bloqueos. `CitaService` no duplica el cálculo de disponibilidad. |
| Envío de notificaciones | Persona responsable del contrato acordado | Preparar y enviar notificaciones únicamente después del commit, conforme al contrato de notificaciones del equipo. |

`CitaService` no ejecuta SQL ni accede directamente a tablas propiedad de Persona 3. Persona 3 no modifica directamente las filas de `cita`: solicita las cancelaciones a través de CitaService.

## 2. Contrato Java propuesto

La firma pública de CitaService se mantiene:

```java
@Transactional(propagation = Propagation.MANDATORY)
public List<CitaAdminCancelacion> cancelarPorBloqueo(
        LocalDateTime desde,
        LocalDateTime hasta
);
```

Y el resultado mínimo:

```java
public record CitaAdminCancelacion(
        Integer citaId,
        Integer usuarioId,
        LocalDateTime inicio,
        LocalDateTime fin
) {}
```

La firma corresponde al método existente. La anotación exige que el llamador tenga ya una transacción activa; no inicia una transacción autónoma.

### Validación y semántica

- Ambos parámetros son obligatorios y deben cumplir `desde.isBefore(hasta)`. Si no, el método falla sin ejecutar consultas ni modificaciones.
- El intervalo es semiabierto `[desde, hasta)`.
- Dos intervalos se solapan si `cita.inicio < hasta && cita.fin > desde`. Si una cita termina exactamente en `desde` o empieza exactamente en `hasta`, no se cancela.
- Se buscan y actualizan únicamente las citas con estado `CONFIRMADA`.
- No se eliminan filas. La operación cambia el estado de la cita a `CANCELADA`.
- El método devuelve los IDs de cita y usuario y el inicio/fin necesarios para preparar notificaciones.
- Si la lectura y la actualización no afectan al mismo número de citas, se informa del fallo para que la transacción pueda revertirse. Esta comprobación detecta inconsistencias observadas, no garantiza exclusión mutua.

## 3. Secuencia transaccional

1. El método público de Persona 3, invocado a través del bean Spring, inicia la transacción JDBC de Spring.
2. Persona 3 crea, modifica o elimina el bloqueo mediante su propio módulo/DAO.
3. Si hay citas afectadas, Persona 3 invoca `citaService.cancelarPorBloqueo(desde, hasta)` usando el bean Spring.
4. `Propagation.MANDATORY` exige que la transacción iniciada por Persona 3 siga activa. CitaService valida el intervalo, busca las citas y solicita al DAO que cancele las `CONFIRMADA` solapadas.
5. Si todas las operaciones terminan correctamente, el método de Persona 3 retorna y Spring confirma conjuntamente los cambios de bloqueos y de citas.
6. Si una operación de base de datos falla antes del commit, la excepción debe propagarse y producir rollback conjunto. No se debe capturar y ocultar un error de forma que el método exterior termine con éxito.
7. Las notificaciones se envían después del commit. Un fallo de correo no revierte las modificaciones persistidas; se registra y comunica según el contrato de notificaciones acordado.

Persona 3 no debe iniciar una transacción distinta alrededor de la cancelación, ni llamar a CitaService desde otro hilo o a través de una referencia que evite el proxy. El envío de correo no forma parte de la transacción de base de datos.

## 4. DataSource y límites de la garantía

La propuesta requiere un único `DataSource` y un único gestor de transacciones JDBC de Spring compartidos por el DAO de Persona 3 y `CitaDao`. Persona 1 debe confirmar la configuración efectiva. Los DAOs JDBC deben usar la integración transaccional de Spring para que cada operación participe en la transacción vinculada.

`MANDATORY` garantiza solamente que existe una transacción activa al entrar en `cancelarPorBloqueo`. No comprueba que la transacción use un recurso concreto ni proporciona exclusión mutua. La transacción común permite que los cambios de bloqueo y cancelación se confirmen o reviertan juntos, pero **no garantiza que nunca ocurran carreras concurrentes ni dobles reservas**. Esa garantía está expresamente fuera del alcance del proyecto.

## 5. Discrepancia pendiente: representación de `bloqueo_horario`

El DDL actual de `database/schema.sql` define `bloqueo_horario` con una fila que incluye:

```text
id, fecha, hora_inicio, hora_fin, motivo, created_at
```

También exige `hora_fin > hora_inicio`. Esta representación describe un intervalo horario por fila. No documenta una fila por cada hueco de 40 minutos ni una clave que identifique individualmente cada hueco.

Por tanto, la afirmación de Persona 3 de que se acordó almacenar una fila por hueco no coincide con el esquema revisado. Este documento no modifica ni da por aprobado un cambio de modelo. Persona 2 y Persona 3 deben confirmar antes de implementar o integrar:

- si una fila representa un intervalo que puede abarcar uno o varios huecos, o
- si se pretende cambiar a una fila por hueco, en cuyo caso se debe presentar y aprobar por separado el cambio de DDL y su impacto en DAO, validaciones y datos existentes.

Hasta cerrar la decisión se respeta el DDL actual y CitaService recibe únicamente el intervalo que se debe usar para cancelar las citas afectadas.

## 6. Pruebas de integración necesarias

Con el DataSource y el gestor transaccional JDBC efectivos del proyecto, comprobar:

1. Persona 3 crea un bloqueo e invoca CitaService a través del proxy; el bloqueo y la cancelación se confirman juntos.
2. Forzar un fallo de base de datos después de una de las modificaciones y verificar desde una conexión independiente que ambos cambios se revirtieron.
3. Invocar `cancelarPorBloqueo` sin transacción y comprobar que `MANDATORY` falla antes de modificar citas.
4. Verificar límites semiabiertos: excluir una cita que termina exactamente en `desde` y otra que empieza exactamente en `hasta`; cancelar las que sí solapan.
5. Verificar que se actualizan únicamente citas `CONFIRMADA` y que no se borran registros ni se vuelven a modificar citas `CANCELADA`.
6. Verificar que el resultado contiene el ID de cita, el ID de usuario y el inicio/fin, sin información adicional innecesaria.
7. Probar el flujo de notificación después del commit: un fallo de correo no cambia el estado ya confirmado de las citas y queda registrado/comunicado.

Las pruebas transaccionales no deben depender de una anotación `@Transactional` del test que oculte el comportamiento del proxy. El caso de rollback debe invocar el servicio sin transacción exterior de test y comprobar el estado persistido desde una conexión independiente.

## 7. Puntos que requieren confirmación

### Persona 1

- Confirmar el `DataSource` único y el `PlatformTransactionManager` JDBC únicos que usarán ambos módulos.
- Confirmar que los DAOs de ambos módulos participan en el mismo recurso transaccional Spring y cómo se configura esto en las pruebas.

### Persona 3

- Confirmar que el servicio de bloqueo abre la transacción exterior e invoca el bean Spring de CitaService.
- Acordar cómo se traduce un bloqueo propio del módulo a los parámetros `[desde, hasta)` de cancelación.
- Resolver con Persona 2 la discrepancia entre intervalo por fila del DDL y la afirmación de una fila por hueco.
- Acordar el contrato de notificaciones: quién recibe el resultado, cómo se asegura el envío después del commit, y cómo se registra/comunica un fallo de correo sin revertir cambios persistidos.
- Confirmar que los errores de persistencia se propagan para que Spring pueda revertir la transacción conjunta.

No forman parte de los requisitos ni de esta propuesta: `GET_LOCK`, locks de sesión, leases, coordinadores, verificadores de cobertura, sincronizaciones de liberación, garantías de serialización o cambios al DDL.
