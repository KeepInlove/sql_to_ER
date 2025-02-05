<!-- ER图显示组件 -->
<template>
  <el-card class="diagram-output" :body-style="{ padding: '0' }">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <i class="el-icon-connection"></i>
          <span>ER 图预览</span>
        </div>
        <div>
          <!-- 点击打开样式设置对话框 -->
          <el-button @click="isStyleDialogVisible = true" icon="el-icon-setting">
            图表样式
          </el-button>
          <!-- 导出按钮 -->
          <el-button type="success" @click="openExportDialog" icon="el-icon-download">
            导出图片
          </el-button>
        </div>
      </div>
    </template>

    <div class="diagram-wrapper">
      <!-- ER图的容器 -->
      <div ref="diagramDiv" class="diagram-container"></div>
    </div>

    <!-- 导出对话框 -->
    <ExportDialog
        v-model="isExportDialogVisible"
        @confirm="handleExportConfirm"
    />

    <!-- 样式设置对话框 -->
    <el-dialog
        v-model="isStyleDialogVisible"
        title="图表样式设置"
        width="400px"
    >
      <el-form :model="diagramSettings" label-width="100px">
        <el-form-item label="显示网格">
          <el-switch v-model="diagramSettings.showGrid" />
        </el-form-item>
        <el-form-item label="全局背景">
          <el-color-picker v-model="diagramSettings.globalBackground" />
        </el-form-item>

        <el-form-item label="全局字体">
          <el-input
              v-model="diagramSettings.globalFont"
              placeholder="如: 微软雅黑, Arial,宋体"
          />
        </el-form-item>

        <el-form-item label="字号">
          <el-input-number v-model="diagramSettings.fontSize" :min="10" :max="60" />
        </el-form-item>

        <el-form-item label="连接线颜色">
          <el-color-picker v-model="diagramSettings.linkColor" />
        </el-form-item>

        <el-form-item label="文字颜色">
          <el-color-picker v-model="diagramSettings.textColor" />
        </el-form-item>

        <el-form-item label="图形底色">
          <el-color-picker v-model="diagramSettings.shapeColor" />
        </el-form-item>

        <el-form-item label="实体底色">
          <el-color-picker v-model="diagramSettings.entityColor" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="isStyleDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="applyStyleAndRebuild">
            应用
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as go from 'gojs'
import ExportDialog from './ExportDialog.vue'
import { defineExpose } from 'vue'
/**
 * DOM 容器的引用
 */
const diagramDiv = ref(null)
/**
 * go.Diagram 实例
 */
let myDiagram = null

/**
 * 导出选项对话框的显示状态
 */
const isExportDialogVisible = ref(false)

/**
 * 样式设置对话框的显示状态
 */
const isStyleDialogVisible = ref(false)

/**
 * 用于存储图表样式可配置项
 */
const diagramSettings = ref({
  globalBackground: '#ffffff', // 全局背景色
  globalFont: '微软雅黑',      // 全局字体
  fontSize: 14,               // 字号
  linkColor: '#1890ff',       // 连接线颜色
  textColor: '#333333',       // 文本颜色
  shapeColor: '#ffffff',      // 属性/关系等节点的底色
  entityColor: '#ffffff',      // 实体节点底色
  showGrid: false // 是否显示网格
})

/**
 * 组件挂载后初始化 Diagram
 */
onMounted(async () => {
  await nextTick()
  initDiagram()
})

/**
 * 初始化 GoJS Diagram
 */
function initDiagram() {
  if (!diagramDiv.value) {
    console.error('图表容器未找到')
    return
  }

  const $ = go.GraphObject.make

  myDiagram = $(go.Diagram, diagramDiv.value, {
    initialContentAlignment: go.Spot.Center,
    'undoManager.isEnabled': true,
    layout: $(go.ForceDirectedLayout, {
      defaultSpringLength: 50,
      defaultElectricalCharge: 100
    }),
    'toolManager.mouseWheelBehavior': go.ToolManager.WheelZoom,
    allowSelect: true,
    allowHorizontalScroll: true,
    allowVerticalScroll: true,
    padding: 40,
    minScale: 0.25,
    maxScale: 2,
    'animationManager.isEnabled': true,
    'grid.visible': false
  })

  applyStyleAndRebuild()
}

/**
 * 根据 diagramSettings 中的配置重建 Node 和 Link 的模板
 */
function applyStyleAndRebuild() {
  if (!myDiagram) return

  const $ = go.GraphObject.make

  // 清空旧模板
  myDiagram.nodeTemplateMap.clear()

  // 设置全局背景
  myDiagram.div.style.background = diagramSettings.value.globalBackground
  // 是否显示网格 (新增)
  myDiagram.grid.visible = diagramSettings.value.showGrid
  // == 实体节点(Entity) ==
  myDiagram.nodeTemplateMap.add(
      'Entity',
      $(
          go.Node,
          'Auto',
          {
            selectionAdornmentTemplate: $(
                go.Adornment,
                'Auto',
                $(
                    go.Shape,
                    { fill: null, stroke: 'blue', strokeWidth: 2 }
                ),
                $(go.Placeholder)
            )
          },
          $(
              go.Shape,
              'Rectangle',
              {
                fill: diagramSettings.value.entityColor, // 实体节点底色
                stroke: diagramSettings.value.linkColor, // 边框颜色
                strokeWidth: 2,
                width: 120,
                height: 60
              }
          ),
          $(
              go.TextBlock,
              {
                margin: 8,
                font: `bold ${diagramSettings.value.fontSize}px ${diagramSettings.value.globalFont}`,
                stroke: diagramSettings.value.textColor,
                editable: true
              },
              new go.Binding('text', 'name').makeTwoWay()
          )
      )
  )

  // == 属性节点(Attribute) ==
  myDiagram.nodeTemplateMap.add(
      'Attribute',
      $(
          go.Node,
          'Auto',
          {
            minSize: new go.Size(60, 40),
            selectionAdornmentTemplate: $(
                go.Adornment,
                'Auto',
                $(
                    go.Shape,
                    { fill: null, stroke: 'blue', strokeWidth: 2 }
                ),
                $(go.Placeholder)
            )
          },
          $(
              go.Shape,
              'Ellipse',
              {
                fill: diagramSettings.value.shapeColor, // 属性节点底色
                stroke: diagramSettings.value.linkColor,
                strokeWidth: 2,
                margin: 4
              }
          ),
          $(
              go.TextBlock,
              {
                margin: 8,
                font: `${diagramSettings.value.fontSize}px ${diagramSettings.value.globalFont}`,
                stroke: diagramSettings.value.textColor,
                editable: true
              },
              new go.Binding('text', 'name').makeTwoWay()
          )
      )
  )

  // == 关系节点(Relationship) ==
  myDiagram.nodeTemplateMap.add(
      'Relationship',
      $(
          go.Node,
          'Auto',
          {
            selectionAdornmentTemplate: $(
                go.Adornment,
                'Auto',
                $(
                    go.Shape,
                    { fill: null, stroke: 'blue', strokeWidth: 2 }
                ),
                $(go.Placeholder)
            )
          },
          $(
              go.Shape,
              'Diamond',
              {
                fill: diagramSettings.value.shapeColor, // 关系菱形底色
                stroke: diagramSettings.value.linkColor,
                strokeWidth: 2,
                width: 60,
                height: 60
              }
          ),
          $(
              go.TextBlock,
              {
                margin: 8,
                font: `${diagramSettings.value.fontSize}px ${diagramSettings.value.globalFont}`,
                stroke: diagramSettings.value.textColor,
                editable: true
              },
              new go.Binding('text', 'name').makeTwoWay()
          )
      )
  )

  // == 连接线模板(Link) ==
  myDiagram.linkTemplate = $(
      go.Link,
      {
        routing: go.Link.Normal,
        corner: 5,
        curve: go.Link.JumpOver,
        selectionAdornmentTemplate: $(
            go.Adornment,
            $(go.Shape, { isPanelMain: true, stroke: 'blue', strokeWidth: 2 })
        )
      },
      $(
          go.Shape,
          {
            strokeWidth: 1.5,
            stroke: diagramSettings.value.linkColor // 连接线颜色
          }
      ),
      $(
          go.TextBlock,
          {
            segmentOffset: new go.Point(0, -10),
            font: `12px ${diagramSettings.value.globalFont}`,
            stroke: diagramSettings.value.textColor,
            editable: true
          },
          new go.Binding('text', 'text').makeTwoWay()
      )
  )

  // 重新布局并缩放
  myDiagram.layoutDiagram(true)
  myDiagram.zoomToFit()

  // 关闭样式设置对话框
  isStyleDialogVisible.value = false
}

/**
 * 打开导出对话框
 */
function openExportDialog() {
  isExportDialogVisible.value = true
}

/**
 * 处理导出对话框 "确认导出" 事件
 */
function handleExportConfirm(options) {
  if (!myDiagram || !myDiagram.nodes.count) return

  // 暂存原背景
  const originalBackground = myDiagram.div.style.background
  // 根据选项设置导出背景
  myDiagram.div.style.background =
      options.background === 'transparent' ? 'transparent' : 'white'

  // 调用 makeImageData 生成图片
  const imageData = myDiagram.makeImageData({
    scale: 1,
    background: options.background === 'transparent' ? null : 'white',
    type: options.format === 'jpeg' ? 'image/jpeg' : 'image/png',
    maxSize: new go.Size(Infinity, Infinity)
  })

  // 生成时间戳用于文件名
  const now = new Date()
  const timestamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(
      now.getDate()
  ).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(
      2,
      '0'
  )}${String(now.getSeconds()).padStart(2, '0')}`

  // 创建下载链接
  const link = document.createElement('a')
  link.download = `${timestamp}ER图.${options.format}`
  link.href = imageData

  // 触发下载
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  // 恢复原背景
  myDiagram.div.style.background = originalBackground
}

/**
 * 从父级组件或外部传入表结构数组进行绘制
 * @param {Array} tables - 表信息列表
 */
function updateDiagram(tables) {
  if (!myDiagram) {
    console.error('图表未初始化')
    return
  }

  const nodeDataArray = []
  const linkDataArray = []

  // 收集所有外键信息，避免重复处理
  const foreignKeyRelations = new Map()
  tables.forEach((table) => {
    table.foreignKeys?.forEach((fk) => {
      if (fk.referenceTable) {
        foreignKeyRelations.set(`${table.tableName}-${fk.referenceTable}`, true)
      }
    })
  })

  tables.forEach((table) => {
    // 添加实体节点
    nodeDataArray.push({
      category: 'Entity',
      key: table.tableName,
      name: removeQuotes(table.comment) || table.tableName
    })

    // 添加属性节点并连线
    table.columns.forEach((col) => {
      const attrKey = `${table.tableName}-${col.name}`
      nodeDataArray.push({
        category: 'Attribute',
        key: attrKey,
        name: removeQuotes(col.comment) || col.name,
        isPrimaryKey: table.primaryKeys?.includes(col.name) || false
      })
      linkDataArray.push({
        from: table.tableName,
        to: attrKey
      })
    })

    // 添加外键关系节点并连线
    table.foreignKeys?.forEach((fk) => {
      const relationKey = `${table.tableName}-${fk.referenceTable}`
      if (fk.referenceTable && foreignKeyRelations.get(relationKey)) {
        const relationshipKey = `${relationKey}-relationship`
        // 关系节点
        nodeDataArray.push({
          category: 'Relationship',
          key: relationshipKey,
          name: '关联'
        })
        // 实体 -> 关系
        linkDataArray.push({
          from: table.tableName,
          to: relationshipKey,
          text: 'N'
        })
        // 关系 -> 另一张表
        linkDataArray.push({
          from: relationshipKey,
          to: fk.referenceTable,
          text: '1'
        })

        // 标记已处理
        foreignKeyRelations.delete(relationKey)
      }
    })
  })

  // 将数据设置到 diagram
  myDiagram.model = new go.GraphLinksModel(nodeDataArray, linkDataArray)
  myDiagram.layoutDiagram(true)
  myDiagram.zoomToFit()
}

/**
 * 工具方法：去除字符串首尾单引号
 * @param {string} str
 * @returns {string}
 */
function removeQuotes(str) {
  if (!str) return str
  return str.replace(/^'|'$/g, '')
}
// 通过 defineExpose 把它暴露出去
defineExpose({
  updateDiagram
})
</script>

<style lang="scss" scoped>
.diagram-output {
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
    position: relative;
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
      color: #67c23a;
    }
  }
}

.diagram-wrapper {
  height: 100%;
  padding: 16px;
  background-color: #f8f9fa;
}

.diagram-container {
  width: 100%;
  height: 100%;
  background-color: white;
  position: relative;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  transition: all 0.3s ease;

  &:hover {
    border-color: #c0c4cc;
  }

  :deep(div[id^="diagram"]) {
    width: 100% !important;
    height: 100% !important;
    position: absolute !important;
    top: 0;
    left: 0;
    background: white;
  }

  :deep(canvas) {
    outline: none;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
