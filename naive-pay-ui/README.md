# Naive‑Pay – Template base para prototipo de medio de pago

Este repositorio es una base con Angular 20 + Tailwind para facilitar la adaptacion al proyecto “Naive‑Pay”, sin necesidad de implementar aún la lógica completa de los módulos.

## A. Presentación (contexto del proyecto)

Naive‑Pay es un prototipo de un medio de pago, cuyo foco principal es el pago de servicios online. Naive‑Pay permite al titular de la cuenta realizar la adquisición de bienes o el pago de servicios vendidos o prestados por los establecimientos comerciales o servicios que acepten este medio de pago y para la realización de pagos en general.

## Cómo personalizar este template

- Se pueden visualizar los ejemplos ya sea sus paginas o componentes, situados en examples, usarlos como guia.

- Utilizar/editar los componentes ubicados en src/app/shared/components. En este directorio se alojan todos los componentes reutilizables.
- Al momento de crear una nueva vista, importar el componente deseado.

## Requisitos

- Node.js 20+
- Angular CLI
- npm

## Instalar

```bash
npm install
```

## Ejecutar

```bash
ng serve
# http://localhost:4200
```

## Estructura relevante

- src/app/app.routes.ts: rutas de la app
- src/app/dashboard: página de ejemplo del dashboard(inicio)
- src/app/examples: rutas de ejemplo, con componentes de ejemplo
- src/app/modulos: modulos cohesivos, con su funcionalidad unica
- public/: assets públicos
