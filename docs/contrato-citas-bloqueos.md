# Contrato de integración: Citas y bloqueos

**Estado:** propuesta para acuerdo entre Persona 2 (Citas) y Persona 3 (Disponibilidad y bloqueos).  
**Alcance:** define responsabilidades y requisitos para cancelar citas afectadas por un bloqueo. No selecciona ni implementa un mecanismo de concurrencia. No autoriza todavía cambios en `CitaService`.

## 1. Responsabilidades

| Responsabilidad | Propietario | Obligación |
|---|---|---|
| Determinar el calendario y los huecos de cita afectados | Persona 3 | Usar la fuente canónica de horarios y apertura, incluidos bloqueos y semántica aprobada de plantillas. No duplicar esta lógica en Citas. |
| Adquirir los locks necesarios | Persona 3 | Adquirir todos los locks requeridos para el intervalo, en un orden global y determinista acordado. |
| Emitir y registrar el lease | Coordinador de Persona 3 | Emitir un lease no falsificable en el uso normal, vinculado a la transacción, conexión, intervalo y conjunto de huecos efectivamente adquiridos. |
| Verificar la cobertura antes de cancelar | Verificador del contrato, implementado por Persona 3 | Rechazar lease ausente, ajeno, expirado, reutilizado, de otra transacción/conexión o con cobertura incompleta. |
| Leer y cancelar citas confirmadas | Persona 2 | Invocar el DAO existente dentro de la transacción validada. Mantener el filtro `CONFIRMADA` y el solapamiento semiabierto `[desde, hasta)`. |
| Liberar los locks | Coordinador de Persona 3 | Liberarlos una sola vez, después del commit o rollback. Persona 2 no los libera. |

La aplicación de Persona 3 debe decidir qué significa un “hueco afectado” según el calendario aprobado. El contrato de Citas no debe reconstruir horarios, inferir fines de semana abiertos ni interpretar `SIN_DATO`.

## 2. API Java propuesta

Los siguientes tipos son orientativos y requieren acuerdo antes de implementarse. Deben residir en un paquete de integración compartido, por ejemplo `es.hair.backend.citas.integration.locks`.

```java
public interface CoordinadorLocksHuecos {
    LeaseLocksHuecos adquirir(LocalDateTime desde, LocalDateTime hasta)
            throws AdquisicionLocksException, TimeoutAdquisicionLocksException;
}

public interface VerificadorLocksHuecos {
    void exigirCobertura(
            LeaseLocksHuecos lease,
            LocalDateTime desde,
            LocalDateTime hasta
    ) throws LeaseLocksInvalidoException, CoberturaLocksIncompletaException;
}

public interface LeaseLocksHuecos {
    // Tipo marcador opaco: no expone setters ni permite al llamador declarar cobertura.
}
```

Contrato futuro del servicio de Citas:

```java
@Transactional(propagation = Propagation.MANDATORY)
public List<CitaAdminCancelacion> cancelarPorBloqueo(
        LocalDateTime desde,
        LocalDateTime hasta,
        LeaseLocksHuecos lease
);
```

El método primero valida `desde < hasta`, luego llama a `VerificadorLocksHuecos.exigirCobertura(...)`, y solo si la verificación termina correctamente consulta y cancela las citas. Debe retirarse la firma sin lease al activar este contrato para que una transacción activa sin evidencia de locks no sea una vía de acceso.

### Excepciones propuestas

- `AdquisicionLocksException`: no se pudo adquirir el conjunto completo; incluye el motivo técnico como causa, sin filtrar credenciales.
- `TimeoutAdquisicionLocksException`: expiró el tiempo de adquisición.
- `LeaseLocksInvalidoException`: lease nulo, no emitido por el coordinador, expirado, ya consumido o ligado a otro contexto.
- `CoberturaLocksIncompletaException`: el lease no cubre todos los huecos requeridos por el intervalo.
- Excepciones JDBC/DAO y excepciones de negocio actuales: propagan el fallo y provocan rollback mientras la transacción está activa.
- `FalloLiberacionLocksException` o señal equivalente de operación: indica que la liberación posterior al cierre transaccional falló. Su tratamiento operativo debe acordarse porque el commit/rollback puede haberse completado ya.

Todas las excepciones de adquisición y validación deben ser no comprobadas (`RuntimeException`) o traducirse a ellas antes de salir del método transaccional, para activar el rollback de Spring.

## 3. Invariantes del lease

1. El lease es opaco: el llamador no puede elegir ni modificar los IDs de locks, el intervalo cubierto, la transacción propietaria ni el estado de adquisición.
2. Solo el coordinador puede emitir un lease aceptable por el verificador. La comprobación no confía únicamente en los campos declarados por el objeto: valida su identidad/nonce contra un registro privado asociado al contexto transaccional.
3. El lease queda ligado a una única transacción y a la conexión física vinculada por Spring. No puede reutilizarse en otra transacción, hilo, conexión o ejecución posterior.
4. El lease representa el conjunto realmente adquirido, no solo el conjunto solicitado.
5. Se valida el intervalo exacto y la cobertura completa. La ampliación o reducción del intervalo después de adquirir los locks invalida el lease para esa llamada.
6. La liberación es de una sola vez y ocurre después de completar la transacción.

Una interfaz Java pública no impide por sí sola que otro código implemente un tipo marcador. La implementación debe verificar la identidad del lease contra estado privado del coordinador y la transacción activa; no basta con `instanceof`, campos públicos o un booleano `locksAdquiridos`.

## 4. Cobertura de un intervalo semiabierto

El intervalo de bloqueo es `[desde, hasta)`, y debe cumplir `desde < hasta`. Las fechas se interpretan como `LocalDateTime` de pared en `Europe/Madrid`, en coherencia con el modelo `DATETIME`; no se convierten implícitamente a UTC.

Para un hueco de cita `[inicioHueco, finHueco)`, el hueco queda afectado si y solo si:

```text
inicioHueco < hasta && finHueco > desde
```

La igualdad en un extremo no constituye solapamiento: un hueco que termina exactamente en `desde` o empieza exactamente en `hasta` queda fuera.

Persona 3 debe generar el conjunto canónico de huecos afectados para todos los días cruzados por el intervalo, considerando únicamente apertura y horarios aprobados. Ese cálculo debe proceder del mismo servicio/regla que usa disponibilidad; no se debe duplicar en Citas. El verificador compara el conjunto canónico requerido con la cobertura registrada en el lease y exige que todos estén presentes. El contrato debe decidir si se permite que el lease cubra huecos adicionales; la recomendación es permitirlos solo si el orden global de adquisición se mantiene y la cobertura solicitada está íntegramente incluida, sin permitir que se omitan huecos requeridos.

Si un bloqueo incluye tramos fuera del horario, estos no generan huecos de cita, aunque el intervalo original se conserva para la cancelación SQL y validación exacta. La política de días sin plantilla debe quedar resuelta por el módulo de Persona 3 antes de producir el conjunto canónico.

## 5. Secuencia transaccional requerida

1. Un servicio orquestador de Persona 3, llamado desde el proxy Spring, inicia una transacción con el gestor acordado.
2. Dentro de esa transacción, Persona 3 valida el intervalo y calcula el conjunto canónico de huecos afectados.
3. El coordinador adquiere todos los locks, con orden determinista. No emite lease parcial.
4. Si la adquisición se completa, el coordinador registra el lease y una sincronización de transacción que liberará los locks en `afterCompletion`, tanto si el resultado fue commit como rollback. La liberación ocurre después de la decisión transaccional y antes de que el recurso de conexión necesario para liberar se cierre o devuelva al pool.
5. Persona 3 invoca `CitaService.cancelarPorBloqueo(desde, hasta, lease)` a través del bean/proxy Spring, dentro de la misma transacción.
6. La propagación `MANDATORY` verifica que hay una transacción activa. El verificador además comprueba lease, propietario transaccional, conexión y cobertura. Una excepción impide acceder a las consultas de cancelación.
7. El DAO de Persona 2 busca y cancela únicamente las citas `CONFIRMADA` que solapan `[desde, hasta)`. La lectura y la actualización participan en la transacción externa.
8. Si todo termina correctamente, el orquestador retorna y el gestor hace commit. Después, la sincronización libera locks.
9. Si cualquier paso transaccional falla, el gestor hace rollback. Después, la sincronización libera locks.

No se deben liberar locks en un `finally` alrededor de `cancelarPorBloqueo`: el método puede retornar antes de que el llamador haga commit. Tampoco se debe hacer commit manual en el coordinador.

El orden exacto de callbacks de sincronización respecto al unbind/cleanup de la conexión es dependiente del mecanismo y del gestor transaccional concretos. Persona 3 debe demostrar con pruebas que la liberación ocurre después del commit/rollback y antes de perder el recurso necesario. No se debe asumir el orden por el mero uso de `TransactionSynchronization`.

## 6. Tratamiento de fallos

| Fallo | Resultado requerido |
|---|---|
| Adquisición falla antes de obtener locks | No se emite lease ni se invoca Citas; rollback. |
| Adquisición parcial | El coordinador libera todas las adquisiciones parciales de forma segura; no emite lease. La transacción falla y no se cancela ninguna cita. Si una liberación parcial falla, el error queda visible y se aplica la política operativa acordada; no se oculta con éxito. |
| Timeout | Tratar como adquisición fallida. No continuar con un conjunto incompleto. |
| Lease nulo, falsificado, de otra transacción o reutilizado | El verificador lanza `LeaseLocksInvalidoException` antes de consultar/actualizar el DAO. Rollback. |
| Cobertura incompleta o intervalo diferente | `CoberturaLocksIncompletaException` antes de consultar/actualizar el DAO. Rollback. |
| Error al buscar/cancelar o inconsistencia en filas | Propagar el error y hacer rollback de las modificaciones de citas. |
| Liberación falla después de commit | El commit no se puede deshacer retroactivamente. Registrar/elevar una alerta operacional; aplicar invalidación de conexión, retry o recuperación según el mecanismo acordado. No reportar la liberación como exitosa. |
| Liberación falla después de rollback | La cancelación de citas sí queda revertida, pero el recurso de lock podría continuar retenido. Aplicar la misma política de contención y alerta. |

La semántica final de adquisición parcial, timeout y liberación depende de la estrategia elegida; no se puede cerrar sin conocer si los locks son de sesión, transaccionales o externos.

## 7. Requisitos de transacción y conexión

- Persona 3 invoca CitaService mediante el bean inyectado por Spring, nunca mediante `new`, auto-invocación de un método del mismo objeto ni una referencia que evite el proxy.
- Coordinador, verificador y `CitaDao` usan el mismo `DataSource` administrado por Spring y el mismo gestor de transacciones.
- Si el mecanismo elegido es dependiente de sesión, la adquisición y liberación deben utilizar la misma conexión física que queda asociada al hilo/transacción. El verificador debe comprobar esa asociación; la mera existencia de una transacción activa no basta.
- Para JDBC imperativo, las operaciones deben integrarse con el recurso vinculado por Spring (por ejemplo, `DataSourceUtils`) y no pedir conexiones independientes al pool para adquirir/verificar mientras DAO utiliza otra.
- No se permite abrir una transacción nueva (`REQUIRES_NEW`), suspender la actual, cambiar de `DataSource` ni ejecutar la cancelación en otro hilo entre adquisición, verificación y DAO.
- Persona 2 no inicia ni confirma una transacción propia en `cancelarPorBloqueo`; `MANDATORY` es obligatorio.
- La identidad de conexión debe comprobarse en runtime mediante un identificador seguro asociado al recurso, no exponiendo credenciales ni depender únicamente de comparar wrappers JDBC.

## 8. Pruebas de integración requeridas

Las pruebas deben usar el mecanismo real que se acuerde y el `DataSource`/pool real de la configuración de integración. Los dobles no demuestran identidad de conexión ni liberación efectiva.

1. **Transacción compartida:** iniciar por el orquestador, adquirir, cancelar y confirmar que DAO observa el mismo contexto; forzar rollback y comprobar que las citas quedan confirmadas.
2. **Identidad de conexión:** capturar en el coordinador, verificador y DAO un identificador de conexión física seguro y demostrar que coincide dentro de la operación. Verificar que la conexión se devuelve al pool una vez terminada la liberación.
3. **Cobertura completa:** probar intervalos que afectan un hueco, varios huecos, límites exactos (termina en `desde`, empieza en `hasta`), varios días y un hueco omitido deliberadamente. El caso incompleto debe fallar antes del DAO.
4. **Lease inválido:** nulo, objeto no emitido por el coordinador, transacción distinta, conexión distinta, intervalo distinto, lease expirado y lease reutilizado. Ningún caso debe consultar ni actualizar citas.
5. **Adquisición parcial/timeout:** simular fallos en distintos puntos del orden de adquisición y comprobar que no se emite lease, que se intenta liberar lo adquirido y que no se cancela ninguna cita.
6. **Commit y rollback:** observar que los locks siguen retenidos hasta finalizar la transacción y se liberan después tanto de commit como de rollback.
7. **Fallo de liberación:** comprobar la señal/alerta, el tratamiento de la conexión y que el fallo no queda oculto como una operación exitosa.
8. **Proxy:** invocar mediante el bean Spring y confirmar `MANDATORY`; una invocación sin transacción debe fallar.
9. **Intervalos:** confirmar que solo se cancelan citas confirmadas con solapamiento semiabierto y que las canceladas no se actualizan de nuevo.

Los tests deben aislar sus datos y verificar el estado desde una conexión independiente cuando sea necesario. La conexión independiente se usa para observar commits/rollbacks, no para realizar la adquisición de locks.

## 9. Información que Persona 3 debe confirmar

Antes de elegir e implementar estrategia, confirmar conjuntamente:

- Versión exacta y topología MySQL: servidor único, clúster/replicación, failover y comportamiento de sesiones al reconectar.
- Pool real, versión y configuración relevante (actualmente la aplicación muestra Hikari en pruebas): tamaño, timeout, validación, max lifetime, idle timeout y comportamiento ante conexiones rotas.
- `DataSource` y `PlatformTransactionManager` efectivos; si son únicos o hay más de uno, y cómo se enlazan recursos.
- Si el lock propuesto dependería de conexión/sesión, transacción o servicio externo, así como alcance de exclusión, timeout y recuperación.
- Cómo detectar pérdida de conexión o invalidación del lease y cómo se impide que una conexión con estado de lock residual vuelva al pool.
- Orden global de locks, duración máxima, política de timeout y estrategia ante adquisición parcial.
- Cálculo canónico de huecos afectados y semántica aprobada de calendario, fines de semana y meses sin plantilla.
- Política de fallo de liberación, alerta, retry/recuperación y criterio para invalidar una conexión.
- Confirmación mediante prueba de integración del orden de callbacks de Spring y del momento de liberación respecto al commit/rollback y al cleanup de conexión.

## 10. Obligatorio frente a decisiones pendientes

### Requisitos obligatorios de este contrato

- Una transacción Spring común abarca adquisición, verificación, consulta/cancelación y resultado commit/rollback.
- La verificación precede a cualquier acceso del DAO para cancelación por bloqueo.
- El lease demuestra propietario, transacción, conexión, intervalo y cobertura; no se acepta una mera bandera.
- Solo Persona 3 calcula los huecos afectados desde la fuente canónica y gestiona la adquisición/liberación.
- Los locks se liberan después de commit o rollback, nunca al retornar `cancelarPorBloqueo`.
- La cancelación mantiene `[desde, hasta)`, cambia solo citas confirmadas y no borra filas.
- Los errores de adquisición, validación, cancelación y liberación son visibles; no existe éxito silencioso.

### Decisiones que requieren acuerdo de Persona 3

- Tecnología concreta de locks, claves y alcance.
- Estructura implementable del lease/registro y cómo vincularlo a la conexión física.
- Servicio responsable de emitir el lease y API definitiva entre módulos.
- Si se permiten locks adicionales a los huecos requeridos y cómo se validan.
- Timeout, orden canónico y limpieza de adquisiciones parciales.
- Callback exacto de liberación y compatibilidad con el pool/gestor transaccional.
- Política de fallo de liberación y recuperación.
- Disponibilidad real de la conexión/lock en la topología y durante failover.

Hasta cerrar estas decisiones y pasar las pruebas de integración descritas, la propuesta no autoriza implementar concurrencia ni integrar `cancelarPorBloqueo` como operación protegida por locks.
