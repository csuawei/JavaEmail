<template>
  <!-- 无需外层容器，直接渲染在 el-main 的 router-view 中 -->
  <div class="inbox-wrapper">
    <!-- 邮件列表操作栏（与主页面布局对齐） -->
    <div class="mail-operate-bar">
      <el-button 
        type="primary" 
        icon="el-icon-refresh" 
        size="mini" 
        @click="refreshMailList"
        :loading="refreshLoading"
      >
        拉取邮件
      </el-button>
      <el-button 
        type="text" 
        icon="el-icon-delete" 
        size="mini" 
        @click="deleteSelectedMails"
        :disabled="selectedMailIds.length === 0"
      >
        删除选中
      </el-button>
      <el-button 
        type="text" 
        icon="el-icon-mark-read" 
        size="mini" 
        @click="markAsRead"
        :disabled="selectedMailIds.length === 0"
      >
        标记为已读
      </el-button>
    </div>

    <!-- 邮件列表容器（贴合 el-main 布局） -->
    <el-card shadow="hover" class="mail-list-card">
      <!-- 加载状态 -->
      <div class="loading-container" v-if="loading">
        <el-icon class="loading-icon"><i class="el-icon-loading"></i></el-icon>
        <span class="loading-text">正在加载邮件...</span>
      </div>

      <!-- 无数据状态 -->
      <div class="empty-container" v-else-if="mailList.length === 0">
        <el-icon class="empty-icon"><i class="el-icon-empty"></i></el-icon>
        <span class="empty-text">暂无邮件</span>
        <el-button 
          type="text" 
          @click="goToWriteLetter"
          class="empty-action-btn"
        >
          去写信
        </el-button>
      </div>

      <!-- 邮件列表 -->
      <el-table
        :data="mailList"
        :row-class-name="tableRowClassName"
        @row-click="goToDetail"
        @selection-change="handleSelectionChange"
        border
        fit
        style="width: 100%;"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column label="发件人" prop="senderAccountEmail" width="180">
          <template slot-scope="scope">
            <span class="sender-text">{{ scope.row.senderAccountEmail || '未知发件人' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="主题" prop="subject">
          <template slot-scope="scope">
            <span class="subject-text">{{ truncateSubject(scope.row.subject) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="摘要" prop="content" width="300">
          <template slot-scope="scope">
            <span class="digest-text">{{ truncateContent(scope.row.content) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发送时间" prop="sendTime" width="160">
          <template slot-scope="scope">
            <span class="date-text">{{ formatMailDate(scope.row.sendTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'Inbox',
  data() {
    return {
      loading: false,
      refreshLoading: false,
      mailList: [],
      userEmail: '',
      userId:'',
      selectedMailIds: [] // 选中的邮件ID（用于批量操作）
    }
  },
  mounted() {
    // 初始化用户信息
    this.initUserInfo()
  },
  methods: {
    /**
     * 初始化用户信息
     */
    initUserInfo() {
      const userInfoStr = sessionStorage.getItem('userInfo')
      if (!userInfoStr) {
        this.$router.push('/login')
        return
      }
      const userInfo = JSON.parse(userInfoStr)
      this.userEmail = userInfo.email
      this.userId = userInfo.userId
      console.log(this.userEmail)
      if (!this.userEmail) {
        this.$message.warning('用户邮箱配置异常，请重新登录')
        this.$router.push('/login')
        return
      }

      // 加载邮件列表
      this.fetchMailList()
    },

    /**
     * 调用后端接口获取邮件列表（适配现有接口路径）
     */
    async fetchMailList() {
      const userInfoStr = sessionStorage.getItem('userInfo')
      const userInfo = JSON.parse(userInfoStr)
      console.log(userInfo)
      this.loading = true
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-recipient/getMailIdByEmail',
          method: 'get',
          params: { email: this.userEmail }
        })

        // （code: "0" 成功）
        if (res.data.code === "0") {
          this.mailList = res.data.data || []
        } else {
          this.mailList = []
          this.$message.warning(res.data.msg || '获取邮件失败')
        }
      } catch (err) {
        this.mailList = []
        console.error('邮件列表请求异常：', err)
        this.$message.error('网络异常，请稍后重试')
      } finally {
        this.loading = false
        this.refreshLoading = false
      }
    },
    /**
     * 拉取邮件
     */
    async pullMailMessage(){
       try {
        const res = await this.$axios({
          url: 'http://localhost:8081/sys-user/pullmail',
          method: "post",                       // 请求方法
          headers: {                            // 请求头
            "Content-Type": "application/json",
          },
          data: {
            id: this.userId,
            email: this.userEmail
          }, 
        })
        // （code: "0" 成功）
        if (res.code === "0") {
          this.$message.warning(res.data.msg || '拉取邮件成功')
        } else {
          this.$message.warning(res.data.msg || '拉取邮件失败')
        }
      } catch (err) {
        console.error('邮件列表请求异常：', err)
        this.$message.error('网络异常，请稍后重试')
      } finally {
        this.loading = false
        this.refreshLoading = false
      }
    },
    /**
     * 刷新邮件列表
     */
    refreshMailList() {
      this.refreshLoading = true
      this.pullMailMessage()
    },

    /**
     * 表格行样式（区分已读/未读）
     */
    tableRowClassName({ row }) {
      return row.readStatus === 0 ? 'mail-unread' : ''
    },

    /**
     * 截断过长主题
     */
    truncateSubject(subject) {
      return subject ? (subject.length > 40 ? `${subject.slice(0, 40)}...` : subject) : '无主题'
    },

    /**
     * 截断过长内容作为摘要
     */
    truncateContent(content) {
      return content ? (content.length > 60 ? `${content.slice(0, 60)}...` : content) : ''
    },

    /**
     * 格式化发送时间
     */
    formatMailDate(time) {
      if (!time) return ''
      const date = new Date(time)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    },

    /**
     * 跳转到邮件详情页（子路由跳转，保持层级一致）
     */
    goToDetail(row) {
      this.$router.push({
        path: '/letter/detail',
        query: { id: row.messageId }
      })
    },

    /**
     * 跳转到写信页（复用现有路由）
     */
    goToWriteLetter() {
      this.$router.push('/letter/write')
    },

    /**
     * 处理表格选中事件（批量操作）
     */
    handleSelectionChange(val) {
      this.selectedMailIds = val.map(item => item.messageId)
    },

    /**
     * 批量删除选中邮件（示例方法，需后端接口配合）
     */
    deleteSelectedMails() {
      if (this.selectedMailIds.length === 0) return

      this.$confirm('确定要删除选中的邮件吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          // 调用后端批量删除接口（需自行实现）
          const res = await this.$axios({
            url: '/mail/batchDelete',
            method: 'post',
            data: { ids: this.selectedMailIds }
          })

          if (res.data.code === "0") {
            this.$message.success('删除成功')
            this.fetchMailList() // 重新加载列表
            this.selectedMailIds = [] // 清空选中
          } else {
            this.$message.warning(res.data.msg || '删除失败')
          }
        } catch (err) {
          console.error('删除邮件异常：', err)
          this.$message.error('删除失败，请稍后重试')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    /**
     * 批量标记为已读（示例方法，需后端接口配合）
     */
    markAsRead() {
      if (this.selectedMailIds.length === 0) return

      try {
        // 调用后端批量标记接口（需自行实现）
        this.$axios({
          url: '/mail/batchMarkRead',
          method: 'post',
          data: { ids: this.selectedMailIds }
        }).then(res => {
          if (res.data.code === "0") {
            this.$message.success('标记成功')
            this.fetchMailList()
            this.selectedMailIds = []
          } else {
            this.$message.warning(res.data.msg || '标记失败')
          }
        })
      } catch (err) {
        console.error('标记邮件异常：', err)
        this.$message.error('标记失败，请稍后重试')
      }
    }
  }
}
</script>

<style scoped>
/* 适配 el-main 布局，无额外边距 */
.inbox-wrapper {
  width: 100%;
  height: 100%;
  padding: 16px 0;
}

/* 邮件操作栏样式 */
.mail-operate-bar {
  margin: 0 16px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 邮件列表卡片样式（贴合 Element UI 表格） */
.mail-list-card {
  margin: 0 16px;
  height: calc(100% - 50px);
  overflow: hidden;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #666;
}

.loading-icon {
  font-size: 32px;
  margin-bottom: 16px;
  animation: el-loading-rotate 1.5s linear infinite;
}

.loading-text {
  font-size: 14px;
}

/* 无数据状态样式 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #999;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  margin-bottom: 20px;
}

.empty-action-btn {
  color: #409eff;
}

/* 表格样式优化 */
.el-table {
  height: calc(100% - 40px);
}

.el-table__body-wrapper {
  max-height: calc(100% - 40px) !important;
}

/* 未读邮件行样式 */
.mail-unread {
  background-color: #fafafa;
}

.mail-unread .subject-text {
  font-weight: 500;
  color: #333;
}

/* 表格文字样式 */
.sender-text {
  font-size: 13px;
  color: #333;
}

.subject-text {
  font-size: 13px;
  color: #666;
}

.digest-text {
  font-size: 12px;
  color: #999;
}

.date-text {
  font-size: 12px;
  color: #999;
}
</style>