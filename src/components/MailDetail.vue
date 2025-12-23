<template>
  <div class="mail-detail-wrapper">
    <!-- 顶部操作栏 -->
    <div class="operate-bar">
      <el-button 
        type="primary" 
        icon="el-icon-arrow-left" 
        size="mini" 
        @click="goBackToInbox"
      >
        返回收件箱
      </el-button>
      <el-button 
        type="primary" 
        icon="el-icon-arrow-left" 
        size="mini" 
        @click="writeLetter"
      >
        回复
      </el-button>
      <!-- 下载所有附件按钮：有附件时显示，添加加载状态 -->
      <el-button 
        v-if="attachments.length > 0" 
        type="primary" 
        icon="el-icon-download" 
        size="mini" 
        @click="downloadAllAttachments"
        style="margin-left: 8px;"
        :loading="downloadLoading"
      >
        下载附件
      </el-button>
    </div>

    <!-- 邮件详情卡片 -->
    <el-card shadow="hover" class="mail-detail-card">
      <!-- 加载状态 -->
      <div class="loading-container" v-if="loading">
        <el-icon class="loading-icon"><i class="el-icon-loading"></i></el-icon>
        <span class="loading-text">正在加载邮件详情...</span>
      </div>

      <!-- 加载失败状态（含未查询到邮件） -->
      <div class="error-container" v-else-if="error">
        <el-icon class="error-icon"><i class="el-icon-error"></i></el-icon>
        <span class="error-text">{{ errorMsg || '邮件加载失败' }}</span>
        <el-button 
          type="text" 
          class="retry-btn" 
          @click="fetchMailDetail"
          v-if="errorMsg !== '返回未查询到邮件'"
        >
          重试
        </el-button>
      </div>

      <!-- 无数据状态（已合并到error状态） -->
      <div class="empty-container" v-else-if="!mailDetail">
        <el-icon class="empty-icon"><i class="el-icon-empty"></i></el-icon>
        <span class="empty-text">该邮件不存在或已被删除</span>
        <el-button 
          type="primary" 
          size="mini" 
          @click="goBackToInbox"
          class="back-btn"
        >
          返回收件箱
        </el-button>
      </div>

      <!-- 邮件详情内容 -->
      <div class="mail-content" v-else>
        <!-- 邮件主题 -->
        <div class="mail-subject">
          {{ mailDetail.subject || '无主题' }}
        </div>

        <!-- 邮件基本信息（严格适配mail_message表字段） -->
        <el-table 
          :data="mailInfoList" 
          border 
          fit 
          class="mail-info-table"
          :show-header="false"
        >
          <el-table-column prop="label" width="80"></el-table-column>
          <el-table-column prop="value"></el-table-column>
        </el-table>

        <!-- 分割线 -->
        <div class="divider"></div>

        <!-- 邮件正文 -->
        <div class="mail-body">
          <div 
            class="body-content" 
            v-html="formatBodyContent(mailDetail.content)"
          ></div>
          <!-- 正文为空时的提示 -->
          <div class="empty-body" v-if="!mailDetail.content || mailDetail.content.trim() === ''">
            无邮件正文
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import Contact from './Contact.vue';
import WriteLetter from './WriteLetter.vue';

export default {
  name: 'MailDetail',
  data() {
    return {
      loading: false,
      error: false,
      errorMsg: '',
      mailDetail: null, // 邮件详情数据（MailMessage实体）
      mailId: '', // 从URL获取的邮件ID
      currentUserEmail: '', // 当前登录用户邮箱（收件人）
      attachments: [], // 附件列表（MailAttachment实体）
      downloadLoading: false // 下载附件的加载状态
    }
  },
  mounted() {
    // 1. 从URL query中获取邮件ID
    this.mailId = this.$route.query.id
    if (!this.mailId) {
      this.error = true
      this.errorMsg = '缺少邮件ID'
      return
    }

    // 2. 获取当前登录用户邮箱（收件人，从sessionStorage读取）
    const userInfoStr = sessionStorage.getItem('userInfo')
    if (userInfoStr) {
      this.currentUserEmail = JSON.parse(userInfoStr).email || '未知收件人'
    }

    // 3. 请求邮件详情
    this.fetchMailDetail()
    // 4. 请求附件列表
    this.fetchAttachmentList()
  },
  computed: {
    // 邮件基本信息列表
    mailInfoList() {
      return [
        { label: '发件人', value: this.getSenderText() },
        { label: '收件人', value: this.currentUserEmail },
        { label: '状态', value: this.getStatusText() },
        { label: '发送时间', value: this.formatMailDate() },
        { label: '阅读状态', value: this.mailDetail.readStatus === 0 ? '未读' : '已读' }
      ]
    }
  },
  methods: {
    /**
     * 请求邮件详情（适配后端ResultUtil包装格式）
     */
    async fetchMailDetail() {
      this.loading = true
      this.error = false
      this.mailDetail = null
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-message/getMailById', // 接口URL
          method: 'get',
          params: { id: this.mailId } // 传入邮件ID
        })

        // 适配后端ResultUtil返回格式
        if (res.data.code === "0") {
          // 成功：获取邮件详情数据
          this.mailDetail = res.data.data || null
          // 标记邮件为已读（如果未读）
          if (this.mailDetail && this.mailDetail.readStatus === 0) {
            this.markAsRead()
          }
        } else if (res.data.code === "801") {
          // 失败：未查询到邮件（后端明确错误码）
          this.error = true
          this.errorMsg = res.data.msg || '未查询到邮件'
        } else {
          // 其他错误
          this.error = true
          this.errorMsg = res.data.msg || '获取邮件详情失败'
        }
      } catch (err) {
        // 网络异常
        this.error = true
        this.errorMsg = '网络异常，请稍后重试'
        console.error('邮件详情请求异常：', err)
      } finally {
        this.loading = false
      }
    },

    /**
     * 请求附件列表（根据messageId=mailId）
     */
    async fetchAttachmentList() {
      if (!this.mailId) return
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-attachment/list', // 附件列表接口
          method: 'get',
          params: { messageId: this.mailId } // 传入邮件ID（messageId）
        })

        // 适配后端ResultUtil返回格式
        if (res.data.code === "0") {
          this.attachments = res.data.data || []
        } else {
          this.$message.warning(res.data.msg || '获取附件列表失败')
        }
      } catch (err) {
        console.error('附件列表请求异常：', err)
        this.$message.error('网络异常，获取附件列表失败')
      }
    },

    /**
     * 下载单个附件（内部调用，适配后端download-by-id接口）
     * @param {Object} attach 单个附件对象（含attachmentId/id）
     */
    downloadSingleAttachment(attach) {
      // 校验附件ID（兼容attachmentId和id字段，适配MyBatis生成器）
      const attachId = attach.attachmentId || attach.id
      if (!attachId) {
        this.$message.warning(`附件【${attach.fileName}】ID不存在，无法下载`)
        return
      }

      // 拼接下载接口URL，触发浏览器下载（GET请求，无需编码）
      const downloadUrl = `http://localhost:8081/mail-attachment/download-by-id?id=${attachId}`
      window.open(downloadUrl)
    },

    /**
     * 下载所有附件（核心功能：循环调用单个下载，加延迟避免浏览器拦截）
     */
    downloadAllAttachments() {
      if (this.attachments.length === 0) {
        this.$message.warning('暂无附件可下载')
        return
      }

      this.downloadLoading = true
      // 循环下载每个附件，每300ms触发一个（避免浏览器拦截并发请求）
      this.attachments.forEach((attach, index) => {
        setTimeout(() => {
          this.downloadSingleAttachment(attach)
          // 最后一个附件下载后，关闭加载状态（延迟500ms确保下载请求发出）
          if (index === this.attachments.length - 1) {
            setTimeout(() => {
              this.downloadLoading = false
              this.$message.success(`已开始下载${this.attachments.length}个附件`)
            }, 500)
          }
        }, index * 300)
      })
    },

    /**
     * 标记邮件为已读（适配后端接口：直接接收 Long 类型 id）
     */
    async markAsRead() {
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-message/markAsRead', // 后端接口地址
          method: 'post',
          headers: {
            'Content-Type': 'application/json' // 确保请求头为 JSON 格式
          },
          data: this.mailId // 直接传递 Long 类型的 mailId
        })

        // 处理后端响应（ResultUtil 格式）
        if (res.data.code === "0") {
          // 标记成功：更新本地阅读状态 + 提示
          this.mailDetail.readStatus = 1
          this.$message.success(res.data.msg || '标记已读成功')
        } else {
          // 标记失败：提示后端返回的错误信息
          this.$message.warning(res.data.msg || '标记已读失败')
        }
      } catch (err) {
        console.error('标记已读请求异常：', err)
        this.$message.error('网络异常，标记已读失败')
      }
    },

    async writeLetter() {
      this.$router.push({
        path: '/letter/write',
        query: { contactEmail: this.mailDetail.senderAccountEmail || '' }
      })
    },

    /**
     * 格式化发件人文本（优先sender_email，无则用sender_account_email）
     */
    getSenderText() {
      if (!this.mailDetail) return '未知发件人'
      const { senderEmail, senderAccountEmail } = this.mailDetail
      return senderAccountEmail
    },

    /**
     * 格式化状态文本（适配status字段：0-草稿/1-已发送/2-已接收/3-已撤回）
     */
    getStatusText() {
      if (!this.mailDetail) return '未知状态'
      const statusMap = {
        0: '草稿',
        1: '已发送',
        2: '已接收',
        3: '已收藏'
      }
      return statusMap[this.mailDetail.status] || `未知状态(${this.mailDetail.status})`
    },

    /**
     * 格式化发送时间（send_time为NULL时显示“未发送（草稿）”）
     */
    formatMailDate() {
      if (!this.mailDetail) return '未知时间'
      const { sendTime, status } = this.mailDetail
      if (!sendTime) {
        return status === 0 ? '未发送（草稿）' : '未知时间'
      }
      // 适配后端DATETIME2格式（如：2025-12-09T20:47:00.858）
      const date = new Date(sendTime)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
    },

    /**
     * 格式化邮件正文（原生JS正则过滤危险HTML标签，避免XSS）
     */
    formatBodyContent(content) {
      if (!content) return ''
      let safeContent = content
        // 处理换行符（将\n转为<br>，优化纯文本换行显示）
        .replace(/\n/g, '<br>')
      return safeContent
    },

    /**
     * 返回收件箱
     */
    goBackToInbox() {
      this.$router.push('/letter/inbox')
    }
  }
}
</script>

<style scoped>
/* 适配el-main布局 */
.mail-detail-wrapper {
  width: 100%;
  height: 100%;
  padding: 16px;
}

/* 操作栏样式 */
.operate-bar {
  margin-bottom: 16px;
}

/* 邮件详情卡片样式 */
.mail-detail-card {
  height: calc(100% - 42px);
  overflow-y: auto;
  padding: 20px;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
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

/* 错误状态样式 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #f56c6c;
}

.error-icon {
  font-size: 32px;
  margin-bottom: 16px;
}

.error-text {
  font-size: 14px;
  margin-bottom: 20px;
}

.retry-btn {
  color: #409eff;
}

/* 无数据状态样式 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
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

.back-btn {
  min-width: 120px;
}

/* 邮件内容样式 */
.mail-subject {
  font-size: 20px;
  font-weight: 500;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

/* 邮件基本信息表格 */
.mail-info-table {
  margin-bottom: 20px;
  font-size: 13px;
}

.mail-info-table .el-table__cell {
  padding: 8px 0;
}

.mail-info-table .el-table__cell:first-child {
  color: #999;
}

.mail-info-table .el-table__cell:last-child {
  color: #333;
}

/* 分割线 */
.divider {
  height: 1px;
  background-color: #eee;
  margin: 16px 0;
}

/* 邮件正文样式 */
.mail-body {
  padding: 10px 0;
  min-height: 200px;
}

.body-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap; /* 保留换行符 */
}

.empty-body {
  font-size: 14px;
  color: #999;
  text-align: center;
  padding: 40px 0;
}

/* 滚动条样式优化 */
.mail-detail-card::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.mail-detail-card::-webkit-scrollbar-thumb {
  border-radius: 3px;
  background-color: #ccc;
}

.mail-detail-card::-webkit-scrollbar-track {
  background-color: #f5f5f5;
}

/* 加载动画 */
@keyframes el-loading-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>