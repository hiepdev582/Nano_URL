// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: "2025-07-15",
  devtools: { enabled: true },

  runtimeConfig: {
    apiServerUrl: process.env.NUXT_API_SERVER_URL || "http://nginx:80",

    public: {
      apiClientUrl: process.env.NUXT_PUBLIC_API_CLIENT_URL || "",
    },
  },
});
