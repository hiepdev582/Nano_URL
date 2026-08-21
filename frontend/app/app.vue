<script setup lang="ts">
// Thiết lập SEO Meta Data
useHead({
  title: "NanoURL - Hệ thống rút gọn link siêu tốc",
  meta: [
    {
      name: "description",
      content:
        "Dịch vụ rút gọn link an toàn, bảo mật và tốc độ cao. Tạo link ngắn miễn phí nhanh chóng.",
    },
    {
      name: "keywords",
      content:
        "rút gọn link, nanourl, url shortener, tinyurl, link ngắn, link rut gon",
    },
    {
      property: "og:title",
      content: "NanoURL - Hệ thống rút gọn link siêu tốc",
    },
    {
      property: "og:description",
      content: "Rút gọn link an toàn, bảo mật và tốc độ cao.",
    },
    { property: "og:type", content: "website" },
  ],
  htmlAttrs: {
    lang: "vi",
  },
});

interface UrlHistoryItem {
  originalUrl: string;
  shortUrl: string;
  shortCode: string;
  createdAt: string;
}

const originalUrl = ref("");
const shortenedUrl = ref("");
const shortCode = ref("");
const isLoading = ref(false);
const errorMessage = ref("");
const history = ref<UrlHistoryItem[]>([]);
const copySuccess = ref(false);

// Config base URL cho API
const config = useRuntimeConfig();

const apiBase = import.meta.server
  ? (config.apiServerUrl as string)
  : (config.public.apiClientUrl as string);

// Bắt đầu rút gọn link
const handleShorten = async () => {
  errorMessage.value = "";
  shortenedUrl.value = "";

  if (!originalUrl.value.trim()) {
    errorMessage.value = "Vui lòng nhập đường dẫn URL cần rút gọn";
    return;
  }

  isLoading.value = true;
  try {
    const response = await $fetch<UrlHistoryItem>("/api/v1/shorten", {
      baseURL: apiBase,
      method: "POST",
      body: { originalUrl: originalUrl.value },
    });

    shortenedUrl.value = response.shortUrl;
    shortCode.value = response.shortCode;

    // Cập nhật lịch sử
    const newEntry = {
      originalUrl: response.originalUrl,
      shortUrl: response.shortUrl,
      shortCode: response.shortCode,
      createdAt: new Date().toLocaleString("vi-VN"),
    };

    // Đẩy lên đầu danh sách và giới hạn 10 bản ghi
    history.value = [
      newEntry,
      ...history.value.filter((h) => h.shortCode !== newEntry.shortCode),
    ].slice(0, 10);
    localStorage.setItem("url_history", JSON.stringify(history.value));
    originalUrl.value = "";
  } catch (err: any) {
    console.error(err);
    if (err && err.data && typeof err.data === "string") {
      errorMessage.value = err.data;
    } else {
      errorMessage.value =
        "Có lỗi xảy ra khi kết nối máy chủ. Vui lòng thử lại!";
    }
  } finally {
    isLoading.value = false;
  }
};

// Sao chép link ngắn
const handleCopy = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    copySuccess.value = true;
    setTimeout(() => {
      copySuccess.value = false;
    }, 2000);
  } catch (err) {
    console.error("Không thể copy", err);
  }
};

// Xóa lịch sử
const clearHistory = () => {
  history.value = [];
  localStorage.removeItem("url_history");
};

// Xóa một dòng trong lịch sử
const deleteHistoryItem = (code: string) => {
  history.value = history.value.filter((item) => item.shortCode !== code);
  localStorage.setItem("url_history", JSON.stringify(history.value));
};

// Load lịch sử từ localStorage
onMounted(() => {
  const localHistory = localStorage.getItem("url_history");
  if (localHistory) {
    try {
      history.value = JSON.parse(localHistory);
    } catch (e) {
      localStorage.removeItem("url_history");
    }
  }
});
</script>

<template>
  <div class="container">
    <!-- Header -->
    <header class="header select-none">
      <div class="logo-wrapper">
        <span class="logo-icon">🔗</span>
        <h1 class="logo-text">Nano<span>URL</span></h1>
      </div>
      <p class="subtitle">Hệ thống rút gọn link an toàn, siêu tốc và ổn định</p>
    </header>

    <!-- Main Card -->
    <main class="main-card glass-panel">
      <form class="input-section" @submit.prevent="handleShorten">
        <div class="input-wrapper">
          <label for="url-input" class="input-prefix">
            {{ "" }}
          </label>
          <input
            v-model="originalUrl"
            type="text"
            id="url-input"
            class="url-input"
            placeholder="Nhập URL cần rút gọn..."
            :disabled="isLoading"
          />
          <button
            type="submit"
            class="btn-submit select-none"
            :disabled="isLoading"
          >
            <span v-if="isLoading" class="spinner"></span>
            <span v-else>Rút gọn</span>
          </button>
        </div>
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      </form>

      <!-- Result Panel -->
      <transition name="fade">
        <div v-if="shortenedUrl" class="result-section">
          <div class="result-card">
            <p class="result-title">Mã rút gọn của bạn đã sẵn sàng</p>
            <div class="result-link-wrapper">
              <a :href="shortenedUrl" target="_blank" class="short-link">{{
                shortenedUrl
              }}</a>
              <div class="action-buttons">
                <button
                  class="btn-action btn-copy"
                  type="button"
                  @click="handleCopy(shortenedUrl)"
                >
                  {{ copySuccess ? "Đã copy ✔" : "Sao chép" }}
                </button>
                <a
                  target="_blank"
                  class="btn-action btn-visit"
                  :href="shortenedUrl"
                  >Truy cập</a
                >
              </div>
            </div>
          </div>
        </div>
      </transition>
    </main>

    <!-- History Panel -->
    <transition name="fade">
      <section v-if="history.length > 0" class="history-section glass-panel">
        <div class="history-header">
          <h2 class="history-title">Lịch sử rút gọn</h2>
          <button class="btn-clear" type="button" @click="clearHistory">
            Xóa toàn bộ
          </button>
        </div>
        <div class="history-list">
          <div
            v-for="item in history"
            :key="item.shortCode"
            class="history-item"
          >
            <div class="history-details">
              <div class="history-urls">
                <a :href="item.shortUrl" target="_blank" class="hist-short">{{
                  item.shortUrl
                }}</a>
                <span class="hist-orig" :title="item.originalUrl">{{
                  item.originalUrl
                }}</span>
              </div>
              <span class="history-date">{{ item.createdAt }}</span>
            </div>
            <div class="history-actions">
              <button
                class="btn-icon"
                title="Sao chép"
                type="button"
                @click="handleCopy(item.shortUrl)"
              >
                📋
              </button>
              <button
                class="btn-icon btn-delete"
                title="Xóa"
                type="button"
                @click="deleteHistoryItem(item.shortCode)"
              >
                🗑️
              </button>
            </div>
          </div>
        </div>
      </section>
    </transition>
  </div>
</template>

<style scoped>
.container {
  width: 100%;
  max-width: 680px;
  padding: 2.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.header {
  text-align: center;
  margin-bottom: 0.5rem;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.logo-icon {
  font-size: 2.8rem;
  filter: drop-shadow(0 0 12px var(--accent-glow));
}

.logo-text {
  font-size: 3rem;
  font-weight: 800;
  letter-spacing: -1.5px;
}

.logo-text span {
  color: var(--accent-primary);
  text-shadow: 0 0 20px var(--accent-glow);
}

.subtitle {
  color: var(--text-secondary);
  font-size: 1.05rem;
  font-weight: 300;
  letter-spacing: 0.5px;
}

/* Card Rút gọn */
.main-card {
  padding: 2.25rem 2rem;
}

.input-wrapper {
  display: flex;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 0.4rem;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.input-wrapper:focus-within {
  border-color: var(--accent-primary);
  box-shadow: 0 0 20px var(--accent-glow);
  background: rgba(15, 23, 42, 0.75);
}

.link-icon {
  font-size: 1.25rem;
  opacity: 0.7;
}

.url-input {
  flex: 1;
  background: transparent;
  border: none;
  padding: 0.8rem 0.5rem;
  color: var(--text-primary);
  font-size: 1rem;
  outline: none;
  font-family: inherit;
  min-width: 0;
  text-overflow: ellipsis;
}

.btn-submit {
  background: linear-gradient(
    135deg,
    var(--accent-primary),
    var(--accent-secondary)
  );
  color: #020617;
  border: none;
  border-radius: 12px;
  padding: 0.8rem 1.75rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  font-family: inherit;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.1);
  box-shadow: 0 4px 15px rgba(56, 189, 248, 0.4);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-text {
  color: var(--danger);
  font-size: 0.85rem;
  margin-top: 0.75rem;
  margin-left: 0.5rem;
}

/* Spinner Loading */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(2, 6, 23, 0.3);
  border-radius: 50%;
  border-top-color: #020617;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Result Section */
.result-section {
  margin-top: 1.75rem;
  padding-top: 1.75rem;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.result-card {
  background: rgba(56, 189, 248, 0.05);
  border: 1px solid rgba(56, 189, 248, 0.18);
  border-radius: 14px;
  padding: 1.25rem;
}

.result-title {
  color: var(--text-secondary);
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 0.75rem;
  font-weight: 600;
}

.result-link-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.short-link {
  color: var(--accent-primary);
  font-size: 1.2rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.25s ease;
  word-break: break-all;
}

.short-link:hover {
  filter: brightness(1.1);
  text-shadow: 0 0 8px var(--accent-glow);
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.btn-action {
  padding: 0.55rem 1rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  font-family: inherit;
}

.btn-copy {
  background: var(--accent-primary);
  color: #020617;
  border: none;
}

.btn-copy:hover {
  filter: brightness(1.1);
  box-shadow: 0 4px 10px rgba(56, 189, 248, 0.2);
}

.btn-visit {
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-primary);
  border: 1px solid rgba(255, 255, 255, 0.12);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-visit:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.25);
}

/* History Section */
.history-section {
  padding: 1.75rem 2rem;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
}

.history-title {
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.btn-clear {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 0.8rem;
  cursor: pointer;
  transition: color 0.2s;
  font-family: inherit;
}

.btn-clear:hover {
  color: var(--danger);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  padding: 0.85rem 1.1rem;
  transition: all 0.25s ease;
}

.history-item:hover {
  border-color: rgba(56, 189, 248, 0.2);
  background: rgba(15, 23, 42, 0.45);
}

.history-details {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex: 1;
  min-width: 0;
  margin-right: 1.5rem;
  gap: 1rem;
}

.history-urls {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.hist-short {
  color: var(--accent-primary);
  font-size: 0.95rem;
  font-weight: 600;
  text-decoration: none;
  word-break: break-all;
}

.hist-orig {
  font-size: 0.78rem;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.history-date {
  font-size: 0.75rem;
  color: var(--text-secondary);
  opacity: 0.8;
  flex-shrink: 0;
  margin-top: 0.2rem;
}

.history-actions {
  display: flex;
  gap: 0.25rem;
  flex-shrink: 0;
}

.btn-icon {
  background: transparent;
  border: none;
  font-size: 1.05rem;
  cursor: pointer;
  padding: 0.35rem 0.5rem;
  border-radius: 6px;
  transition: background 0.2s;
}

.btn-icon:hover {
  background: rgba(255, 255, 255, 0.06);
}

.btn-delete:hover {
  background: rgba(248, 113, 113, 0.1);
}

/* Animations */
.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.4s ease,
    transform 0.4s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Responsive */
@media (max-width: 600px) {
  .container {
    padding: 1.5rem 1rem;
  }
  .main-card {
    padding: 1.5rem 1.25rem;
  }
  .input-wrapper {
    flex-direction: column;
    padding: 0.5rem;
    align-items: stretch;
    gap: 0.5rem;
  }
  .input-prefix {
    display: none;
  }
  .url-input {
    padding: 0.6rem 0.5rem;
  }
  .btn-submit {
    padding: 0.75rem;
    width: 100%;
  }
  .result-link-wrapper {
    flex-direction: column;
    align-items: stretch;
  }
  .action-buttons {
    justify-content: flex-end;
  }
  .history-details {
    flex-direction: column;
    gap: 0.4rem;
  }
  .hist-orig {
    max-width: 220px;
  }
}

.select-none {
  user-select: none;
}
</style>
