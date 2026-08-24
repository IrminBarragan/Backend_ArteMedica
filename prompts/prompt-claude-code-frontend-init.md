Vas a crear desde cero el frontend de **ArteMédica Farmacia**: Vite + React + TypeScript, usando **pnpm** como gestor de paquetes (no uses `npm` ni `yarn` en ningún paso). El backend ya existe y está documentado en `../docs` — si no tienes acceso a esa carpeta en este repo, pide que te compartan `../docs/API.md`, `../docs/MODELOS.md` y `../docs/AUTENTICACION.md` del backend antes de continuar, los vas a necesitar para tipar correctamente las peticiones.

Trabaja las 3 tareas en orden y **haz un commit al terminar cada una**. Verifica que el proyecto compile y levante (`pnpm dev`) sin errores antes de cada commit.

---

### TAREA 1 — Inicializar el proyecto

```bash
pnpm create vite@latest . -- --template react-ts
pnpm install
```

Usa `.` como nombre de proyecto para inicializarlo en el directorio actual (raiz del repo).

Verifica que se haya generado `pnpm-lock.yaml` (no `package-lock.json` ni `yarn.lock`) y que `pnpm dev` levante el proyecto por defecto de Vite sin errores.

Commit:
```bash
git add .
git commit -m "chore: inicializar proyecto Vite + React + TypeScript con pnpm"
```

---

### TAREA 2 — Dependencias, tema y estructura de carpetas

Instala el stack del proyecto:
```bash
pnpm add react-router-dom axios @tanstack/react-query
pnpm add @mui/material @emotion/react @emotion/styled @mui/icons-material @mui/x-data-grid
pnpm add react-hook-form @hookform/resolvers zod
pnpm add @mui/x-date-pickers date-fns
```

**Variables de entorno**: crea `../.env` y `.env.example` (este último sin valores sensibles, para que quede versionado) con:
```
VITE_API_URL=http://localhost:8080/api
```

**Alias de importación**: configura `@/` apuntando a `../src` tanto en `vite.config.ts` (usando `resolve.alias`) como en `tsconfig.json` (`compilerOptions.paths`), para poder importar como `@/components/...` en vez de rutas relativas largas.

**Estructura de carpetas**: crea (vacías salvo lo que se pide en la Tarea 3):
```
src/
├── api/
├── components/
├── context/
├── hooks/
├── pages/
├── routes/
├── types/
└── theme.ts
```

**`src/theme.ts`**: crea un tema de MUI usando los colores corporativos reales de ArteMédica Hospital (si tienes acceso a `../docs` o a algún manual de marca del proyecto, confirma los valores exactos ahí; si no, usa estos, que son los oficiales):
```ts
// Azul Mediterráneo (primario): #264272
// Azul Medieval (secundario): #62b5e5
// Vivanco (acento/fondo): #eccfb4
```
Usa `createTheme` de MUI, define `palette.primary.main = '#264272'`, `palette.secondary.main = '#62b5e5'`, y define la tipografía base (puedes usar la fuente por defecto del sistema, no hace falta replicar "Calibri" del manual de marca, eso era para el logotipo, no para la tipografía de UI).

**`src/types/`**: crea los archivos de tipos TypeScript que reflejen los DTOs reales del backend (`catalogos.types.ts`, `inventario.types.ts`, `compras.types.ts`, `solicitudes.types.ts`, `auth.types.ts`). Usa `../docs/MODELOS.md` del backend como fuente de verdad para los campos exactos — no inventes campos que no estén ahí. Como mínimo necesitas ya tipados: `LoginRequestDTO`, `LoginResponseDTO`, y los enums (`Rol`, `TipoEmpleado`, `EstatusSolicitud`, `TipoMovimiento`, `OrigenMovimiento`) como TypeScript `type` de valores literales (ej. `type Rol = 'ADMIN' | 'MEDICO' | 'FARMACEUTICO'`).

Commit:
```bash
git add .
git commit -m "chore: agregar dependencias, alias, tema MUI y estructura de carpetas"
```

---

### TAREA 3 — Cliente HTTP, autenticación y rutas protegidas

**`src/api/client.ts`** — instancia de axios:
```ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```
Ajusta el manejo del 401 si prefieres usar `useNavigate` de React Router en vez de `window.location.href` (es más "React-friendly", pero fuera de un componente no tienes acceso al hook directamente — puedes usar un patrón de "navigate function" inyectada si quieres evitar el hard reload; si es más complejidad de la necesaria por ahora, el `window.location.href` es válido y simple).

**`src/api/auth.api.ts`**: función `login(credenciales: LoginRequestDTO): Promise<LoginResponseDTO>` que hace `POST /auth/login` con `apiClient` (sin el interceptor de token, porque este endpoint es público — igual funciona con la instancia normal, el interceptor solo agrega el header si existe token en localStorage, y en login todavía no hay).

**`src/context/AuthContext.tsx`**:
- Estado: `usuario` (username, rol, empleadoId — lo que devuelva `LoginResponseDTO` menos el token), `token`, `isAuthenticated` (derivado).
- Al montar, lee `token`/`usuario` de `localStorage` si existen, para persistir la sesión al refrescar la página.
- `iniciarSesion(credenciales)`: llama a `login(...)`, guarda `token` y `usuario` en `localStorage` y en el estado del contexto.
- `cerrarSesion()`: limpia `localStorage` y el estado, redirige a `/login`.
- Expón un hook `useAuth()` que devuelva el contenido del contexto (lanza error si se usa fuera del `AuthProvider`).

**`src/routes/ProtectedRoute.tsx`**: componente que recibe `rolesPermitidos?: Rol[]` como prop opcional y sus `children` (o usa el patrón de `<Outlet />` de React Router 6, como prefieras):
- Si no hay sesión (`!isAuthenticated`) → redirige a `/login`.
- Si `rolesPermitidos` está definido y el rol del usuario no está en la lista → redirige a una página `/no-autorizado` (créala como un componente simple por ahora, un mensaje centrado).

**`src/App.tsx`** (o donde defina las rutas): configura `BrowserRouter`, `QueryClientProvider` (con un `QueryClient` nuevo), `ThemeProvider` con el tema de la Tarea 2, y `AuthProvider` envolviendo todo. Define las rutas:
- `/login` → pública (crea una página placeholder simple por ahora, un formulario mínimo sin estilizar del todo, se pule en el siguiente bloque de trabajo).
- `/` → protegida, cualquier rol autenticado, muestra un layout base con un mensaje "Bienvenido a ArteMédica Farmacia" como placeholder de dashboard.
- `/no-autorizado` → pública, mensaje simple.

**`src/components/Layout.tsx`**: estructura base con `AppBar` de MUI arriba (muestra el nombre del usuario logueado y su rol, con un botón de "Cerrar sesión" que llame a `cerrarSesion()`) y un espacio para contenido (`<Outlet />`). No hace falta el menú lateral con todas las opciones de navegación todavía — eso se arma cuando existan las páginas reales en el siguiente bloque de trabajo, deja el `Layout` listo para recibir esos items después.

Commit:
```bash
git add .
git commit -m "feat: agregar cliente HTTP con JWT, AuthContext, rutas protegidas y layout base"
```

---

No hagas `git push`. Deja los commits listos localmente para revisión.
