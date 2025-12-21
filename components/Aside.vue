<template>
  <el-menu
    default-active="/letter/inbox" 
    class="left-menu"
    mode="vertical" 
    router 
    background-color="#2d3748"
    text-color="#fff"
    active-text-color="#42b983" 
  >
    <!-- 菜单标题 -->
    <el-menu-item index="/letter/write" class="menu-item">
      <i class="el-icon-edit" slot="icon"></i> <!-- 写信图标 -->
      <span slot="title">写信</span>
    </el-menu-item>

    <el-menu-item index="/letter/inbox" class="menu-item">
      <i class="el-icon-inbox" slot="icon"></i> <!-- 收件箱图标 -->
      <span slot="title">收件箱</span>
    </el-menu-item>

    <el-menu-item index="/letter/outbox" class="menu-item">
      <i class="el-icon-send" slot="icon"></i> <!-- 发件箱图标 -->
      <span slot="title">发件箱</span>
    </el-menu-item>

    <el-menu-item index="/letter/draft" class="menu-item">
      <i class="el-icon-document" slot="icon"></i> <!-- 草稿箱图标 -->
      <span slot="title">草稿箱</span>
    </el-menu-item>

    <el-menu-item index="/contact" class="menu-item">
      <i class="el-icon-user" slot="icon"></i> <!-- 联系人图标 -->
      <span slot="title">联系人</span>
    </el-menu-item>
  </el-menu>
</template>

<script>
export default {
  name: 'LeftMenu'
}
</script>

<style scoped>
/* 菜单栏样式：固定左侧，设置宽度和高度 */
.left-menu {
  width: 200px; /* 菜单宽度 */
  height: 100%; /* 高度 = 视口高度 - Header高度（60px） */
  position: fixed; /* 固定左侧 */
  left: 0;
  z-index: 99; /* 确保不被覆盖 */
}

/* 菜单项间距调整 */
.menu-item {
  margin: 10px 0;
}
</style>