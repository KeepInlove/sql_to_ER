<template>
  <el-dialog
      v-model="dialogVisible"
      title="导出选项"
      width="400px"
      @close="handleClose"
  >
    <el-form :model="exportOptions" label-width="120px">
      <el-form-item label="图片格式">
        <el-select v-model="exportOptions.format" placeholder="请选择图片格式">
          <el-option label="PNG" value="png" />
          <el-option label="JPEG" value="jpeg" />
        </el-select>
      </el-form-item>
      <el-form-item label="背景">
        <el-radio-group v-model="exportOptions.background">
          <el-radio label="transparent">透明背景</el-radio>
          <el-radio label="white">白色背景</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认导出</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { ref, watch } from 'vue'

export default {
  name: 'ExportDialog',
  props: {
    // v-model 绑定布尔值，控制对话框显示与隐藏
    modelValue: {
      type: Boolean,
      required: true
    }
  },
  emits: ['update:modelValue', 'confirm'],
  setup(props, { emit }) {
    const dialogVisible = ref(props.modelValue)
    const exportOptions = ref({
      format: 'png',            // 图片格式
      background: 'transparent' // 背景（transparent / white）
    })

    // 监听父组件传来的值，更新本组件状态
    watch(
        () => props.modelValue,
        (newVal) => {
          dialogVisible.value = newVal
        }
    )

    // 监听 dialogVisible 并同步回父组件
    watch(dialogVisible, (newVal) => {
      emit('update:modelValue', newVal)
    })

    const handleClose = () => {
      dialogVisible.value = false
    }

    const handleConfirm = () => {
      // 将当前选项返回给父组件
      emit('confirm', exportOptions.value)
      dialogVisible.value = false
    }

    return {
      dialogVisible,
      exportOptions,
      handleClose,
      handleConfirm
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
