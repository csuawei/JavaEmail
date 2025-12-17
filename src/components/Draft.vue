<template>
  <div class="inbox-wrapper">
    <!-- 草稿箱操作栏 -->
    <div class="mail-operate-bar">
      <el-button 
        type="primary" 
        icon="el-icon-refresh" 
        size="mini" 
        @click="refreshDraftList"
        :loading="refreshLoading"
      >
        刷新草稿
      </el-button>
      <el-button 
        type="text" 
        icon="el-icon-delete" 
        size="mini" 
        @click="deleteSelectedDrafts"
        :disabled="selectedDraftIds.length === 0"
      >
        删除选中
      </el-button>
      <el-button 
        type="primary" 
        icon="el-icon-edit" 
        size="mini" 
        @click="editSelectedDraft"
        :disabled="selectedDraftIds.length !== 1"
      >
        编辑选中
      </el-button>
    </div>

    <!-- 草稿列表容器 -->
    <el-card shadow="hover" class="mail-list-card">
      <!-- 加载状态 -->
      <div class="loading-container" v-if="loading">
        <el-icon class="loading-icon"><i class="el-icon-loading"></i></el-icon>
        <span class="loading-text">正在加载草稿...</span>
      </div>

      <!-- 无数据状态 -->
      <div class="empty-container" v-else-if="draftList.length === 0">
        <el-icon class="empty-icon"><i class="el-icon-empty"></i></el-icon>
        <span class="empty-text">暂无草稿</span>
        <el-button 
          type="text" 
          @click="goToWriteLetter"
          class="empty-action-btn"
        >
          去写新邮件
        </el-button>
      </div>

      <!-- 草稿列表 -->
      <el-table
        :data="draftList"
        @row-click="goToEdit"
        @selection-change="handleSelectionChange"
        border
        fit
        style="width: 100%;"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column label="发件人" prop="senderAccountEmail" width="180">
          <template slot-scope="scope">
            <span>{{ scope.row.senderAccountEmail || '未知' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收件人" prop="senderEmail" width="180">
          <template slot-scope="scope">
            <span>{{ scope.row.senderEmail || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="主题" prop="subject">
          <template slot-scope="scope">
            <span>{{ truncateSubject(scope.row.subject) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="保存时间" prop="draftTime" width="160">
          <template slot-scope="scope">
            <span>{{ formatDraftDate(scope.row.draftTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'Draft',
  data() {
    return {
      loading: false,
      refreshLoading: false,
      draftList: [],
      userEmail: '',
      userId: '',
      selectedDraftIds: [] // 选中的草稿ID
    }
  },
  mounted() {
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
      
      if (!this.userEmail) {
        this.$message.warning('用户邮箱配置异常，请重新登录')
        this.$router.push('/login')
        return
      }

      // 加载草稿列表
      this.fetchDraftList()
    },

    /**
     * 获取草稿列表
     */
    async fetchDraftList() {
      this.loading = true
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-message/getDraftsByEmail',
          method: 'get',
          params: { email: this.userEmail }
        })

        if (res.data.code === "0") {
          this.draftList = res.data.data || []
           //console.log('草稿列表第一条数据：', this.draftList[0]); 
        } else {
          this.draftList = []
          this.$message.warning(res.data.msg || '获取草稿失败')
        }
      } catch (err) {
        this.draftList = []
        console.error('获取草稿列表异常：', err)
        this.$message.error('网络异常，请稍后重试')
      } finally {
        this.loading = false
        this.refreshLoading = false
      }
    },

    /**
     * 刷新草稿列表
     */
    refreshDraftList() {
      this.refreshLoading = true
      this.fetchDraftList()
    },

    /**
     * 截断过长主题
     */
    truncateSubject(subject) {
      return subject ? (subject.length > 40 ? `${subject.slice(0, 40)}...` : subject) : '无主题'
    },

    /**
     * 格式化保存时间
     */
    formatDraftDate(time) {
      if (!time) return ''
      const date = new Date(time)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    },

    /**
     * 跳转到编辑草稿页
     */
    goToEdit(row) {
        console.log('点击的草稿行数据：', row); 
        console.log('草稿ID是否存在：', row.id); // 重点看这行！
        this.$router.push({
            path: '/letter/draft-write', // 改为新的草稿编辑页路由
            query: { draftId: row.messageId }
        })
    },

    /**
     * 跳转到新建邮件页
     */
    goToWriteLetter() {
      this.$router.push('/letter/write')
    },

    /**
     * 处理选中事件
     */
    handleSelectionChange(val) {
      this.selectedDraftIds = val.map(item => item.id)
    },

    /**
     * 编辑选中的草稿
     */
    editSelectedDraft() {
      if (this.selectedDraftIds.length !== 1) return
      
      const draftId = this.selectedDraftIds[0]
      this.$router.push({
        path: '/letter/write',
        query: { draftId }
      })
    },

    /**
     * 批量删除选中草稿
     */
    deleteSelectedDrafts() {
      if (this.selectedDraftIds.length === 0) return

      this.$confirm('确定要删除选中的草稿吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$axios({
            url: 'http://localhost:8081/mail-message/batchDeleteDrafts',
            method: 'post',
            data: { ids: this.selectedDraftIds }
          })

          if (res.data.code === "0") {
            this.$message.success('删除成功')
            this.fetchDraftList()
            this.selectedDraftIds = []
          } else {
            this.$message.warning(res.data.msg || '删除失败')
          }
        } catch (err) {
          console.error('删除草稿异常：', err)
          this.$message.error('删除失败，请稍后重试')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    }
  }
}
</script>

<style scoped>
/* 复用与收件箱相同的样式 */
.inbox-wrapper {
  width: 100%;
  height: 100%;
  padding: 16px 0;
}

.mail-operate-bar {
  margin: 0 16px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mail-list-card {
  margin: 0 16px;
  height: calc(100% - 50px);
  overflow: hidden;
}

.loading-container,
.empty-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading-icon,
.empty-icon,
.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.loading-text,
.empty-text,
.error-text {
  color: #606266;
  font-size: 14px;
}

.empty-action-btn {
  margin-top: 16px;
}
</style>