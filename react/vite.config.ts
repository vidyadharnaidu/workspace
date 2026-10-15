import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";

const USERS: Record<string, { id: number; name: string }> = {
  "1": { id: 1, name: "Ada Lovelace" },
  "2": { id: 2, name: "Grace Hopper" },
  "3": { id: 3, name: "Margaret Hamilton" },
};

function mockUsersApi(): Plugin {
  return {
    name: "mock-users-api",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = "url" in req ? String(req.url ?? "") : "";
        const match = url.match(/^\/api\/users\/(\d+)/);
        if (!match) {
          next();
          return;
        }
        const id = match[1];
        const user = USERS[id];
        setTimeout(() => {
          res.setHeader("Content-Type", "application/json");
          if (!user) {
            res.statusCode = 404;
            res.end(JSON.stringify({ error: "not found" }));
            return;
          }
          res.end(JSON.stringify(user));
        }, 350);
      });
    },
  };
}

export default defineConfig({
  root: "playground",
  plugins: [react(), mockUsersApi()],
  server: {
    port: 5173,
    open: true,
  },
});
