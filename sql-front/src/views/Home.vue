<template>
  <div class="home">
    <el-container>
      <el-header height="80px">
        <div class="header-content">
          <div class="logo">
            <i class="el-icon-data-analysis"></i>
            <h1>SQL to ER</h1>
          </div>
          <div class="header-right">
            <a href="https://github.com/yourusername/sql-to-er" target="_blank" class="github-link">
              <i class="el-icon-platform-eleme"></i>
              <span>GitHub</span>
            </a>
          </div>
        </div>
      </el-header>
      <el-main>
        <div class="main-content">
          <el-row :gutter="20">
            <el-col :span="12">
              <sql-editor @parse-sql="handleParseSql" />
            </el-col>
            <el-col :span="12">
              <er-diagram ref="erDiagram" @export="handleExportDiagram" />
            </el-col>
          </el-row>
        </div>
      </el-main>
    </el-container>

    <export-dialog
      v-model="exportDialogVisible"
      @confirm="handleExport"
    />
  </div>
</template>

<script>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import * as go from 'gojs'
import SqlEditor from '../components/SqlEditor.vue'
import ErDiagram from '../components/ErDiagram.vue'
import ExportDialog from '../components/ExportDialog.vue'

export default {
  name: 'HomePage',
  components: {
    SqlEditor,
    ErDiagram,
    ExportDialog
  },
  setup() {
    const erDiagram = ref(null)
    const exportDialogVisible = ref(false)
    let currentDiagram = null

    const handleParseSql = async (sql) => {
      if (!sql.trim()) {
        ElMessage.warning('请输入SQL建表语句')
        return
      }

      try {
        // 打印原始SQL语句
        console.log('发送的原始SQL语句:', sql)

        // 发送原始SQL语句到服务器
        const response = await axios.post('http://localhost:8080/api/er-diagram/parse', 
          { sql },
          {
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json'
            },
            withCredentials: true
          }
        )
        
        // 打印后端返回的数据
        console.log('后端返回的数据:', response.data)
        
        if (response.data && Array.isArray(response.data) && response.data.length > 0) {
          erDiagram.value.updateDiagram(response.data)
          ElMessage.success('ER图生成成功')
        } else {
          ElMessage.warning('未能识别有效的建表语句，请检查SQL语法')
        }
      } catch (error) {
        console.error('完整错误信息:', error)
        if (error.response) {
          ElMessage.error(error.response.data.message || '生成ER图失败，请检查SQL语句格式')
        } else {
          ElMessage.error('服务器连接失败，请确保后端服务正在运行')
        }
      }
    }

    const handleExportDiagram = (diagram) => {
      // 处理导出逻辑
      const dataUrl = diagram.makeImageData({
        scale: 1,
        background: "white"
      })
      
      const link = document.createElement('a')
      link.download = 'er-diagram.png'
      link.href = dataUrl
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }

    const handleExport = (options) => {
      if (!currentDiagram) {
        ElMessage.warning('图表未初始化')
        return
      }

      try {
        const bounds = currentDiagram.documentBounds.copy()
        const margin = 10

        const imageData = currentDiagram.makeImageData({
          scale: 1,
          background: options.background,
          type: `image/${options.format}`,
          size: new go.Size(bounds.width + 2 * margin, bounds.height + 2 * margin)
        })

        const a = document.createElement('a')
        a.style.display = 'none'
        a.href = imageData
        a.download = `ER图.${options.format}`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        
        ElMessage.success('图片已导出')
      } catch (error) {
        console.error('导出图片时发生错误:', error)
        ElMessage.error('导出图片失败：' + error.message)
      }
    }

    return {
      erDiagram,
      exportDialogVisible,
      handleParseSql,
      handleExportDiagram,
      handleExport
    }
  }
}
</script>

<style lang="scss" scoped>
.home {
  min-height: 100vh;
  background-color: #f0f2f5;
}

.el-header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  
  i {
    font-size: 32px;
    color: #409eff;
  }
  
  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.header-right {
  .github-link {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #606266;
    text-decoration: none;
    font-size: 16px;
    transition: color 0.3s ease;
    
    &:hover {
      color: #409eff;
    }
    
    i {
      font-size: 20px;
    }
  }
}

.el-main {
  padding: 20px;
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
}

.el-row {
  margin-bottom: 0 !important;
}
</style> 