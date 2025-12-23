<template>
  <div style="display: flex; line-height: 40px; justify-content: space-between; width: 100%; padding: 0 20px;">
    <!-- 搜索框区域 -->
    <div style="width: 300px;">
      <el-input
        placeholder="搜索邮件"
        prefix-icon="el-icon-search"
        v-model="input"
        size="small"
      ></el-input>
    </div>

    <!-- 用户信息与操作区域 -->
    <div style="display: flex; align-items: center;">
      <!-- 当前邮箱显示（若存在） -->
      <span v-if="user.email" style="margin-right: 15px; color: #666;">
        {{ user.email }}
      </span>
      
      <!-- 用户下拉菜单 -->
      <el-dropdown>
        <span style="cursor: pointer;">
          {{ user.username }}
          <i class="el-icon-arrow-down" style="margin-left: 5px;"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item @click.native="switchMailAccount">
            <i class="el-icon-swap" style="margin-right: 5px;"></i>切换邮箱账户
          </el-dropdown-item>
          <el-dropdown-item @click.native="logout">
            <i class="el-icon-switch-button" style="margin-right: 5px;"></i>退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      user: JSON.parse(sessionStorage.getItem('userInfo')) || { username: '未知用户' },
      input: ''
    }
  },
  methods: {

    // 切换邮箱账户 - 跳转到邮箱账户管理页面
    switchMailAccount() {
      this.$router.push('/mail/account').catch(err => {
        console.log('路由跳转异常:', err);
      });
    },

    // 退出登录
    logout() {
      this.$confirm('确认退出登录?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        center: true
      }).then(() => {
        // 清除会话存储
        sessionStorage.removeItem('userInfo');
        // 跳转到登录页
        this.$router.push('/login');
        // 显示成功提示
        this.$message({
          type: 'success',
          message: '退出登录成功'
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消退出'
        });
      });
    }
  },
  watch: {
    // 监听用户信息变化（如登录状态更新）
    '$route'(to) {
      if (to.path === '/login') {
        this.user = { username: '未知用户' };
      } else {
        const userInfo = sessionStorage.getItem('userInfo');
        if (userInfo) {
          this.user = JSON.parse(userInfo);
        }
      }
    }
  }
}
</script>

<style scoped>
/* 调整下拉菜单样式 */
::v-deep .el-dropdown-menu {
  min-width: 160px;
}

::v-deep .el-dropdown-menu__item {
  padding: 8px 15px;
}

/* 搜索框聚焦样式优化 */
::v-deep .el-input__inner:focus {
  border-color: #42b983;
}
</style>