<template>
  <section class="panel video-panel" v-loading="loading">
    <div class="panel-header"><div><div class="panel-title">平台管理</div><div class="panel-desc">维护平台连接器和可采集能力；停用后不会出现在默认筛选中。</div></div><el-button type="primary" @click="newPlatform">新增平台</el-button></div>
    <el-table :data="platforms" empty-text="暂无平台配置" style="width:100%">
      <el-table-column prop="displayName" label="平台" min-width="150"><template #default="{ row }"><strong>{{ row.displayName }}</strong><div class="table-sub">{{ row.platformCode }}</div></template></el-table-column>
      <el-table-column prop="connectorName" label="连接器" min-width="200" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="form.platformCode ? '编辑平台' : '新增平台'" width="min(620px, 94vw)" destroy-on-close>
      <el-form label-position="top"><el-form-item label="平台代码"><el-input v-model.trim="form.platformCode" :disabled="editing" placeholder="例如 kuaishou" /></el-form-item><el-form-item label="显示名称"><el-input v-model.trim="form.displayName" /></el-form-item><el-form-item label="连接器名称"><el-input v-model.trim="form.connectorName" placeholder="例如 kuaishou-approved-export-v1" /></el-form-item><el-form-item label="可用能力"><el-checkbox-group v-model="selectedCapabilities"><el-checkbox v-for="item in capabilityOptions" :key="item.key" :value="item.key">{{ item.label }}</el-checkbox></el-checkbox-group></el-form-item><el-form-item><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePlatformConfig, fetchPlatformConfigs, savePlatformConfig } from '@/api/platform'
const loading=ref(false), saving=ref(false), platforms=ref([]), dialogVisible=ref(false), editing=ref(false)
const capabilityOptions=[['comments','评论'],['replies','回复'],['danmaku','弹幕'],['reviews','评分/剧评'],['series','剧集层级'],['completion_rate','完播率'],['creator_metrics','账号指标'],['official_heat','官方热度']].map(([key,label])=>({key,label}))
const form=reactive({platformCode:'',displayName:'',connectorName:'',capabilities:{},enabled:true})
const selectedCapabilities=computed({get:()=>Object.entries(form.capabilities).filter(([,v])=>v).map(([k])=>k),set:(keys)=>{form.capabilities=Object.fromEntries(capabilityOptions.map(({key})=>[key,keys.includes(key)]))}})
onMounted(load)
async function load(){loading.value=true;try{platforms.value=await fetchPlatformConfigs()}catch(e){ElMessage.error(e.message||'平台配置加载失败')}finally{loading.value=false}}
function reset(){Object.assign(form,{platformCode:'',displayName:'',connectorName:'',capabilities:{},enabled:true})}
function newPlatform(){reset();editing.value=false;dialogVisible.value=true}
function edit(row){Object.assign(form,{...row,capabilities:{...row.capabilities}});editing.value=true;dialogVisible.value=true}
async function save(){if(!form.platformCode||!form.displayName||!form.connectorName){ElMessage.warning('请填写平台代码、显示名称和连接器名称');return}saving.value=true;try{await savePlatformConfig({...form});ElMessage.success('平台配置已保存');dialogVisible.value=false;await load()}catch(e){ElMessage.error(e.message||'平台配置保存失败')}finally{saving.value=false}}
async function remove(row){
  try{
    const { value: password }=await ElMessageBox.prompt(`删除“${row.displayName}”后无法恢复。请输入管理密码确认删除。`, '确认删除平台', {confirmButtonText:'确认删除',cancelButtonText:'取消',inputType:'password',inputPlaceholder:'请输入管理密码',inputValidator:(value)=>value ? true : '必须输入管理密码',type:'warning'})
    await deletePlatformConfig(row.platformCode,password)
    ElMessage.success('平台已删除');await load()
  }catch(error){if(error !== 'cancel' && error !== 'close') ElMessage.error(error.message||'平台删除失败')}
}
</script>
