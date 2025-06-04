import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import viteCompression from 'vite-plugin-compression'
import path from 'path'

// `mode` 是从命令行参数 `--mode` 传入的值
export default defineConfig(({ mode }) => {
  // 使用 `mode` 从项目根目录加载相应的环境变量
  const env = loadEnv(mode, process.cwd(), 'VITE_');

  return {
    base: env.VITE_USER_NODE_ENV === '' ? '/dolphinscheduler/ui/' : '/',
    plugins: [
      vue(),
      vueJsx(),
      viteCompression({
        verbose: true,
        disable: false,
        threshold: 10240,
        algorithm: 'gzip',
        ext: '.gz',
        deleteOriginFile: false
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    server: {
      proxy: {
        '/dolphinscheduler': {
          // 使用加载的环境变量
          target: env.VITE_APP_DEV_WEB_URL,
          changeOrigin: true
        }
      }
    }
  };
});
