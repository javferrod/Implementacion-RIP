#Introducción

Implementación para RO del protocolo RIP v2, protocolo que descubre las subredes y los caminos más cortos
hacia ellas mediante un algoritmo de vector de distancias.

#Adyacencias y rutas directamente conectadas

Debido a las especificaciones del propio proyecto, las subredes directamente conectadas y la adyacencia entre routers
se definen mediante un archivo de configuración denominado
> ripconfig-A.B.C.D.txt
Donde A.B.C.D es la ip de la maquina en la interfaz eth0.

#Funcionamiento
El funcionamiento es el mismo que el definido en el [estándar](https://tools.ietf.org/html/rfc2453). Las características
implementadas son las siguientes:
- Split Horizon con Poison Reverse
- Triggered Updates: *Presenta algunos leves fallos*
- Autentificación con contraseña simple
- Respuesta a peticiones: *Nunca probado*

El programa respeta rigurosamente los tiempos especificados por el estándar.
Los paquetes creados con este programa cumplen la especificación de RIP v2 y es compatible con cualquier programa que 
emplee este mismo protocolo.

La única desviación del estándar se realiza al anunciar y recibir rutas con métrica inferior a 1 (concretamente 0) por
indicación del profesor.

#Estructura

Se emplean 4 thread:
- Receiver: Para recibir paquetes, que se interrumpirá cada vez que se tenga que enviar uno. 
- Sender: Para enviar los paquetes ordinarios cada 30 segundos
- TriggeredUpdates: Emplea el modelo consumidor para esperar por rutas a incluir en un Triggered Update 
mediante una blocking queue
- Table: Almacena los datos y a la vez vigila que las rutas cumplan con los tiempos de expiración y 
recoleccion de basura
