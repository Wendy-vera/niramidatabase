# 📘 MANUAL DE USUARIO — Sistema NIRAMI

**Versión:** 1.0  
**Última actualización:** 2026-06-25  
**Sistema:** Nirami — Plataforma de Comercio Electrónico Multivengedor

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Acceso al Sistema](#2-acceso-al-sistema)
3. [Perfil de Usuario (Todos los Roles)](#3-perfil-de-usuario)
4. [Rol: CLIENTE](#4-rol-cliente)
5. [Rol: VENDEDOR](#5-rol-vendedor)
6. [Rol: ADMINISTRADOR](#6-rol-administrador)
7. [Preguntas Frecuentes](#7-preguntas-frecuentes)
8. [Glosario](#8-glosario)

---

## 1. Introducción

**Nirami** es una plataforma de comercio electrónico multivengedor donde:

- Los **clientes** pueden explorar y comprar productos de distintos vendedores.
- Los **vendedores** publican productos, gestionan su inventario y reciben pagos.
- El **administrador** supervisa toda la plataforma: usuarios, productos, ventas y pagos.

### Tecnología
- Aplicación web Java EE desplegada en Apache Tomcat.
- Base de datos MySQL (`nirami_prueba`).
- Interfaz responsive diseñada con CSS y JSP.

---

## 2. Acceso al Sistema

### 2.1 Iniciar Sesión

1. Abrir el navegador y acceder a la URL del sistema (ej: `http://localhost:8080/nirami/`).
2. Hacer clic en el botón **"Iniciar Sesión"** o navegar a `/login`.
3. Ingresar el **correo electrónico** y la **contraseña**.
4. Presionar **"Entrar"**.

> ⚠️ **IMPORTANTE:** Si las credenciales son incorrectas o la cuenta está inactiva, se mostrará un mensaje de error. Verifique su correo y contraseña.

El sistema redirige automáticamente según el rol:
| Rol | Redirección |
|---|---|
| CLIENTE | Catálogo de productos (`/`) |
| VENDEDOR | Panel del vendedor (`/vendedor/dashboard`) |
| ADMINISTRADOR | Panel de administración (`/admin/dashboard`) |

### 2.2 Registrarse (solo Clientes y Vendedores)

1. En la página de login, clic en **"¿No tienes cuenta? Regístrate"**.
2. Completar el formulario:
   - **Nombre completo**
   - **Correo electrónico** (será su usuario de acceso)
   - **Teléfono** (opcional)
   - **Contraseña** (mínimo 8 caracteres)
   - **Tipo de cuenta:** Cliente o Vendedor
   - Para **Vendedores**: número de cuenta bancaria (obligatorio para recibir pagos)
   - Para **Clientes**: dirección de envío (opcional, se puede actualizar después)
3. Presionar **"Registrarse"**.
4. Será redirigido al login para ingresar con sus nuevas credenciales.

### 2.3 Cerrar Sesión

Hacer clic en el botón **"Salir"** disponible en el menú de navegación superior desde cualquier página.

---

## 3. Perfil de Usuario

Disponible para todos los roles en `/perfil`.

### Cómo actualizar el perfil

1. Iniciar sesión.
2. En el menú superior, hacer clic en **"Perfil"**.
3. Se mostrará la información actual del usuario.
4. Modificar los campos deseados:
   - Nombre
   - Teléfono
   - Correo electrónico
   - Contraseña (dejar en blanco para no cambiarla)
   - Foto de perfil (subir imagen)
   - Dirección de envío (Clientes) / Cuenta bancaria (Vendedores)
5. Presionar **"Guardar Cambios"**.

---

## 4. Rol: CLIENTE

Los clientes pueden navegar el catálogo, guardar favoritos y realizar compras.

### 4.1 Explorar el Catálogo

1. Al iniciar sesión (o sin sesión), el catálogo principal muestra todos los productos aprobados.
2. **Buscar productos:** Ingresar términos en la barra de búsqueda y presionar Enter.
3. **Filtrar por categoría:** En el panel lateral o menú de categorías, seleccionar una categoría.
4. Cada producto muestra: imagen, nombre, precio, vendedor y botones de acción.

> 📊 **Historial automático:** Cada vez que un cliente ve un producto, busca o compra, el sistema registra esa acción automáticamente para ayudar al administrador a entender el comportamiento de compra.

### 4.2 Ver Detalle de un Producto

1. En el catálogo, hacer clic en el nombre o imagen del producto.
2. Se muestra la página de detalle con: descripción completa, precio, imágenes y vendedor.
3. Desde aquí se puede:
   - **Agregar al carrito** con la cantidad deseada.
   - **Agregar/quitar de favoritos** con el botón ♥.

### 4.3 Carrito de Compras

1. Acceder al carrito desde el ícono 🛒 en el menú superior.
2. Ver todos los productos agregados con sus cantidades y subtotales.
3. Modificar cantidades o eliminar productos.
4. Ver el resumen: subtotal, IVA (19%) y total.
5. Presionar **"Proceder al Pago"** para ir al checkout.

### 4.4 Realizar una Compra (Checkout)

1. Desde el carrito, presionar **"Proceder al Pago"**.
2. Confirmar o ingresar la dirección de envío.
3. Revisar el resumen del pedido.
4. Presionar **"Confirmar Compra"**.
5. Si la compra es exitosa, se muestra una página de confirmación.

> 📌 **Nota:** El stock se reduce automáticamente al confirmar la compra. Si no hay stock suficiente, el sistema lo notificará.

La orden queda en estado **PENDIENTE** hasta que el administrador la apruebe o rechace.

### 4.5 Historial de Órdenes

- Acceder a **"Mis Pedidos"** desde el menú de navegación.
- Ver todas las órdenes con su estado: PENDIENTE, APROBADO o RECHAZADO.

### 4.6 Lista de Favoritos

1. Acceder a **"Mis Favoritos"** desde el menú.
2. Ver todos los productos guardados como favoritos.
3. Hacer clic en ♥ en cualquier producto para agregarlo/quitarlo.
4. Desde la lista de favoritos se puede acceder al detalle de cada producto.

---

## 5. Rol: VENDEDOR

Los vendedores gestionan su inventario, publican productos y ven sus ventas y pagos.

### 5.1 Panel Principal del Vendedor

Acceder a `/vendedor/dashboard`. Muestra:
- Resumen de productos activos e inventario actual.
- Ventas recientes.
- Alertas de stock bajo.

### 5.2 Gestionar Productos

#### Publicar un nuevo producto

1. En el panel, hacer clic en **"Agregar Producto"**.
2. Completar el formulario:
   - **Nombre del producto** (obligatorio)
   - **Descripción detallada**
   - **Precio de venta**
   - **Categoría** (seleccionar de las disponibles)
   - **Stock inicial** (cantidad disponible)
   - **Imágenes** (al menos una imagen principal)
3. Presionar **"Publicar"**.
4. El producto queda en estado **"EN REVISIÓN"** hasta que el administrador lo apruebe.

#### Editar un producto

1. En el inventario, localizar el producto.
2. Hacer clic en **"Editar"**.
3. Modificar los campos necesarios.
4. Presionar **"Guardar"**.

### 5.3 Gestionar Inventario (Stock)

1. En el panel, ver la tabla de inventario con el stock actual de cada producto.
2. Actualizar el stock desde el formulario de edición del producto.

> ⚠️ **IMPORTANTE:** El stock del vendedor se reduce automáticamente cuando un cliente realiza una compra. Si el stock llega a 0, el producto deja de aparecer disponible para compra.

### 5.4 Ver Ventas

1. Ir a **"Ventas"** en el menú (`/vendedor/ventas`).
2. Ver una lista de todas las ventas de sus productos con:
   - Fecha de la orden
   - Producto vendido
   - Cantidad y precio
   - Cliente (si aplica)
   - Estado de la orden

### 5.5 Ver Pagos Recibidos

1. Ir a **"Pagos"** en el menú (`/vendedor/pagos`).
2. Se muestran tres indicadores principales:
   - 💰 **Ingresos Totales (Ventas):** Monto bruto total de todas sus ventas.
   - ✅ **Pagos Completados:** Monto ya transferido a su cuenta bancaria.
   - ⏳ **Pagos Pendientes:** Monto pendiente de transferencia por el administrador.
3. La tabla de detalle muestra cada transacción con:
   - Fecha de la orden
   - Producto vendido
   - Subtotal (ingreso bruto)
   - Monto neto (después de comisiones, si aplica)
   - Estado del pago: **PENDIENTE** o **PAGADO**

> 📌 **Nota:** Cuando el administrador marca un pago como realizado, el estado cambia de **PENDIENTE** a **PAGADO** con la fecha de pago registrada. Este cambio se refleja inmediatamente en su panel.

---

## 6. Rol: ADMINISTRADOR

El administrador tiene acceso completo a todas las funciones del sistema.

### 6.1 Panel de Revisiones de Productos

URL: `/admin/dashboard`

1. Ver todos los productos en estado **"EN REVISIÓN"**.
2. Para cada producto, puede:
   - Ver el detalle completo (imágenes, descripción, precio).
   - **Aprobar:** El producto pasa a estado APROBADO y aparece en el catálogo.
   - **Rechazar:** El producto no se publica y el vendedor debe corregirlo.

### 6.2 Gestión de Usuarios

#### Ver Vendedores
URL: `/admin/vendedores`

Lista de todos los vendedores registrados con: nombre, correo, teléfono, estado y fecha de registro.

#### Ver Clientes
URL: `/admin/clientes`

Lista de todos los clientes registrados. En la columna **"Historial"**, hacer clic en 📋 **"Ver"** para acceder al historial completo de actividades de ese cliente.

### 6.3 Historial de Actividad de Clientes

URL: `/admin/historial-usuario?idCliente={id}`

Muestra **todas las acciones** realizadas por un cliente específico:

| Acción | Ícono | Descripción |
|---|---|---|
| VER_PRODUCTO | 👁️ | El cliente visitó la página de un producto |
| BÚSQUEDA | 🔍 | El cliente buscó un término en el catálogo |
| AGREGAR_FAVORITO | ❤️ | El cliente agregó un producto a favoritos |
| QUITAR_FAVORITO | 💔 | El cliente quitó un producto de favoritos |
| COMPRAR | 🛒 | El cliente realizó una compra |

Se muestra además:
- Fecha y hora exacta de la acción
- Producto involucrado (si aplica)
- Término de búsqueda (si es una búsqueda)
- Dirección IP de origen

**Estadísticas rápidas:** Conteo de vistas, búsquedas, interacciones con favoritos y compras.

### 6.4 Gestión de Categorías

URL: `/admin/categorias`

- Crear nuevas categorías para organizar los productos.
- Activar o desactivar categorías existentes.
- Las categorías activas aparecen en el catálogo para que los clientes las usen como filtro.

### 6.5 Gestión de Ventas (Historial Global)

URL: `/admin/ventas`

Vista global de **todas las ventas** de la plataforma con filtros:
- **Por producto:** Ver ventas de un producto específico.
- **Por vendedor:** Ver ventas de un vendedor específico.
- **Por cliente:** Ver compras de un cliente específico.

Columnas de la tabla:
| Columna | Descripción |
|---|---|
| Orden # | ID único de la orden |
| Fecha | Fecha y hora de la orden |
| Cliente | Nombre del comprador |
| Producto | Nombre del producto vendido |
| Vendedor | Nombre del vendedor responsable |
| Cant. | Cantidad de unidades |
| Precio U. | Precio unitario al momento de la venta |
| Subtotal | Total de la línea (precio × cantidad) |
| Estado Pago | Estado actual de la orden |
| Acciones | Botones para Aprobar/Rechazar (solo órdenes PENDIENTES) |

#### Aprobar un pago (orden)

1. Localizar la orden en estado **PENDIENTE**.
2. Hacer clic en **"✓ Aprobar"**.
3. Confirmar el diálogo de confirmación.
4. El sistema actualiza:
   - Estado de la orden → **APROBADO**
   - Estado de la orden → **COMPLETADA**
   - Pago al vendedor → **PAGADO** (con fecha de pago)

#### Rechazar un pago (orden)

1. Localizar la orden en estado **PENDIENTE**.
2. Hacer clic en **"✗ Rechazar"**.
3. Confirmar el diálogo. 
4. El sistema:
   - Cambia estado de orden → **RECHAZADO** / **CANCELADA**
   - Restaura el stock del vendedor automáticamente
   - Deja el pago en estado **PENDIENTE** (sin fecha de pago)

### 6.6 Gestión de Pagos a Vendedores

URL: `/admin/pagos-vendedores`

> ⭐ Esta es la vista principal para gestionar los pagos pendientes a vendedores.

**Acceso:** Desde el menú de administración → **"💰 Pagos"** o desde la vista de Ventas → botón **"💸 Gestionar Pagos a Vendedores"**.

#### Panel de resumen

Tres tarjetas de estadísticas:
- **Vendedores con pagos pendientes:** Número de vendedores que tienen dinero pendiente.
- **Total pendiente de pago:** Suma total de todos los montos pendientes.
- **Transacciones pendientes:** Número total de ventas sin pagar.

#### Tabla de pagos por vendedor

Cada fila representa un vendedor con pagos pendientes:
| Columna | Descripción |
|---|---|
| Vendedor | Nombre e ID del vendedor |
| Cuenta Bancaria | Número de cuenta donde realizar la transferencia |
| Transacciones Pendientes | Cantidad de ventas individuales sin pagar |
| Monto Total a Pagar | Suma de todos los montos netos pendientes |
| Acción | Botón **💸 Pagar** |

#### Cómo realizar un pago a un vendedor

1. Localizar al vendedor en la tabla.
2. Verificar la **cuenta bancaria** de destino.
3. Hacer clic en **"💸 Pagar"**.
4. Se muestra un diálogo de confirmación detallado con:
   - Nombre del vendedor
   - Cuenta bancaria destino
   - Monto total a pagar
5. Presionar **"Aceptar"** en el diálogo de confirmación.
6. El sistema marca **TODAS** las transacciones pendientes del vendedor como **PAGADAS** con la fecha y hora actual.
7. Se muestra un mensaje de confirmación: "✅ Pago realizado exitosamente a [Nombre Vendedor]".

> 🔔 **El vendedor verá el cambio inmediatamente** en su panel de Pagos (`/vendedor/pagos`): las transacciones aparecerán en estado **PAGADO** con la fecha de pago registrada.

> ⚠️ **IMPORTANTE:** El sistema registra que el pago fue realizado, pero **NO ejecuta transferencias bancarias automáticas**. El administrador debe realizar la transferencia real a la cuenta bancaria indicada antes (o durante) de presionar el botón "Pagar".

---

## 7. Preguntas Frecuentes

**¿Puedo recuperar mi contraseña olvidada?**  
Actualmente el sistema no tiene recuperación de contraseña automática. Contactar al administrador del sistema.

**¿Por qué mi producto no aparece en el catálogo?**  
Los productos nuevos deben ser revisados y aprobados por el administrador. El estado cambia de "EN REVISIÓN" a "APROBADO" cuando el administrador lo apruebe.

**¿Por qué mi compra quedó en estado PENDIENTE?**  
Todas las compras comienzan en estado PENDIENTE hasta que el administrador las revise y apruebe. Una vez aprobadas, el estado cambia a APROBADO y el vendedor recibe la notificación de pago.

**¿Cómo sé que el vendedor me pagó?**  
Como vendedor, el estado de cada transacción en su panel de Pagos cambia de PENDIENTE a PAGADO cuando el administrador confirma el pago. También se registra la fecha exacta del pago.

**¿Puedo ver qué acciones hizo un cliente específico?**  
Sí, como administrador. Ir a la lista de Clientes y hacer clic en "📋 Ver" en la columna Historial del cliente que desea consultar.

**¿Se puede deshacer un pago a un vendedor?**  
No. Una vez que el administrador marca un pago como realizado (PAGADO), no se puede revertir desde la interfaz actual. Contactar al desarrollador del sistema si necesita esta funcionalidad.

---

## 8. Glosario

| Término | Definición |
|---|---|
| **Orden** | Registro de una compra completa realizada por un cliente |
| **Detalle de Orden** | Línea individual de la orden (un producto específico con cantidad y precio) |
| **Estado PENDIENTE** | Orden creada pero no aprobada aún por el administrador |
| **Estado APROBADO** | Orden confirmada y pago al vendedor registrado |
| **Estado RECHAZADO** | Orden no procesada; el stock se restaura automáticamente |
| **Pago al Vendedor** | Registro en `pagos_vendedor` que indica cuánto debe pagar el admin al vendedor por una venta |
| **Monto Neto** | Monto final a pagar al vendedor después de descontar la comisión de la plataforma |
| **Comisión** | Porcentaje que retiene la plataforma Nirami por cada venta (actualmente: 0%) |
| **Stock Local** | Cantidad disponible de un producto en el inventario del vendedor específico |
| **Historial de Usuario** | Registro cronológico de todas las acciones de un cliente (vistas, búsquedas, favoritos, compras) |
| **IVA** | Impuesto al Valor Agregado, calculado como el 19% del subtotal |
| **bcrypt** | Algoritmo de encriptación usado para almacenar contraseñas de forma segura |

---

*Manual de Usuario — Nirami Platform v1.0 | © 2026 Nirami Dev Team*
