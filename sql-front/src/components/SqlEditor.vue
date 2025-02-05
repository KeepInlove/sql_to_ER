<!-- SQL输入组件 -->
<template>
  <el-card class="sql-input" :body-style="{ padding: '0' }">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <i class="el-icon-edit-outline"></i>
          <span>SQL 输入</span>
        </div>
        <el-button type="primary" @click="handleParseSql" :icon="ArrowRight">
          生成 ER 图
        </el-button>
      </div>
    </template>
    <div class="editor-container">
      <el-input
        v-model="sqlInput"
        type="textarea"
        :rows="15"
        placeholder="请在此输入 CREATE TABLE 语句..."
        resize="none"
        spellcheck="false"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { defineEmits } from 'vue'

const sqlInput = ref('')
const emit = defineEmits(['parse-sql'])

const handleParseSql = () => {
  emit('parse-sql', sqlInput.value)
}
</script>

<style lang="scss" scoped>
.sql-input {
  height: calc(100vh - 140px);
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  
  &:hover {
    box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
  }

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #ebeef5;
    background: #fff;
  }
  
  :deep(.el-card__body) {
    height: calc(100% - 60px);
    padding: 0;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;

    i {
      font-size: 18px;
      color: #409eff;
    }
  }
}

.editor-container {
  height: 100%;
  padding: 16px;
  background-color: #f8f9fa;

  :deep(.el-textarea) {
    height: 100%;

    .el-textarea__inner {
      height: 100%;
      padding: 16px;
      font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
      font-size: 14px;
      line-height: 1.6;
      color: #2c3e50;
      background-color: #fff;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      transition: all 0.3s ease;

      &:hover {
        border-color: #c0c4cc;
      }

      &:focus {
        border-color: #409eff;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
      }
    }
  }
}
</style> 