import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppShell from '../layouts/AppShell.vue'
import LoginView from '../views/login/LoginView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import LeaveListView from '../views/oa/leaves/LeaveListView.vue'
import LeaveDetailView from '../views/oa/leaves/LeaveDetailView.vue'
import LeaveFormView from '../views/oa/leaves/LeaveFormView.vue'
import ExpenseListView from '../views/oa/expenses/ExpenseListView.vue'
import ExpenseDetailView from '../views/oa/expenses/ExpenseDetailView.vue'
import ExpenseFormView from '../views/oa/expenses/ExpenseFormView.vue'
import SealListView from '../views/oa/seals/SealListView.vue'
import SealDetailView from '../views/oa/seals/SealDetailView.vue'
import SealFormView from '../views/oa/seals/SealFormView.vue'
import PurchaseListView from '../views/oa/purchases/PurchaseListView.vue'
import PurchaseDetailView from '../views/oa/purchases/PurchaseDetailView.vue'
import PurchaseFormView from '../views/oa/purchases/PurchaseFormView.vue'
import TodoListView from '../views/workflow/TodoListView.vue'
import TodoDoneView from '../views/workflow/TodoDoneView.vue'
import ApplicationListView from '../views/workflow/ApplicationListView.vue'
import CcListView from '../views/workflow/CcListView.vue'
import WorkflowTemplatesView from '../views/workflow/WorkflowTemplatesView.vue'
import WorkflowInstancesView from '../views/workflow/WorkflowInstancesView.vue'
import WorkflowExceptionsView from '../views/workflow/WorkflowExceptionsView.vue'
import WorkflowSimulatorView from '../views/workflow/WorkflowSimulatorView.vue'
import SystemConfigsView from '../views/system/SystemConfigsView.vue'
import SystemDictsView from '../views/system/SystemDictsView.vue'
import SystemNumberRulesView from '../views/system/SystemNumberRulesView.vue'
import SystemWorkCalendarView from '../views/system/SystemWorkCalendarView.vue'
import SystemImportExportView from '../views/system/SystemImportExportView.vue'
import FormTemplatesView from '../views/form/FormTemplatesView.vue'
import FormVersionsView from '../views/form/FormVersionsView.vue'
import FormDesignerView from '../views/form/FormDesignerView.vue'
import FormFieldRulesView from '../views/form/FormFieldRulesView.vue'
import ReportWorkflowEfficiencyView from '../views/report/ReportWorkflowEfficiencyView.vue'
import ReportTodosView from '../views/report/ReportTodosView.vue'
import ReportLeavesView from '../views/report/ReportLeavesView.vue'
import ReportExpensesView from '../views/report/ReportExpensesView.vue'
import ReportContractsView from '../views/report/ReportContractsView.vue'
import ReportAssetsView from '../views/report/ReportAssetsView.vue'
import ReportUsersView from '../views/report/ReportUsersView.vue'
import ContractListView from '../views/contract/ContractListView.vue'
import ContractDetailView from '../views/contract/ContractDetailView.vue'
import ContractFormView from '../views/contract/ContractFormView.vue'
import NoticeListView from '../views/notice/NoticeListView.vue'
import NoticeDetailView from '../views/notice/NoticeDetailView.vue'
import NoticeFormView from '../views/notice/NoticeFormView.vue'
import MeetingRoomListView from '../views/meeting/MeetingRoomListView.vue'
import MeetingBookingListView from '../views/meeting/MeetingBookingListView.vue'
import AssetListView from '../views/asset/AssetListView.vue'
import AssetDetailView from '../views/asset/AssetDetailView.vue'
import SupplyListView from '../views/supply/SupplyListView.vue'
import SupplyRecordsView from '../views/supply/SupplyRecordsView.vue'
import MessageListView from '../views/message/MessageListView.vue'
import FileLibraryView from '../views/file/FileLibraryView.vue'
import OrgDeptView from '../views/org/OrgDeptView.vue'
import OrgUserView from '../views/org/OrgUserView.vue'
import PermissionRoleView from '../views/permission/PermissionRoleView.vue'
import PermissionMenuView from '../views/permission/PermissionMenuView.vue'
import PermissionButtonView from '../views/permission/PermissionButtonView.vue'
import PermissionTempAuthView from '../views/permission/PermissionTempAuthView.vue'
import PermissionPreviewView from '../views/permission/PermissionPreviewView.vue'
import RuleListView from '../views/rule/RuleListView.vue'
import RuleDetailView from '../views/rule/RuleDetailView.vue'
import RuleSimulatorView from '../views/rule/RuleSimulatorView.vue'
import AuditLoginLogsView from '../views/audit/AuditLoginLogsView.vue'
import AuditOperationLogsView from '../views/audit/AuditOperationLogsView.vue'
import AuditPermissionLogsView from '../views/audit/AuditPermissionLogsView.vue'
import AuditRuleLogsView from '../views/audit/AuditRuleLogsView.vue'
import AuditFileDownloadLogsView from '../views/audit/AuditFileDownloadLogsView.vue'
import FileDetailView from '../views/file/FileDetailView.vue'
import FileRecycleBinView from '../views/file/FileRecycleBinView.vue'
import OpsHealthView from '../views/ops/OpsHealthView.vue'
import OpsOnlineUsersView from '../views/ops/OpsOnlineUsersView.vue'
import OpsJobLogsView from '../views/ops/OpsJobLogsView.vue'
import OpsExceptionsView from '../views/ops/OpsExceptionsView.vue'
import OpsBackupsView from '../views/ops/OpsBackupsView.vue'
import AccountProfileView from '../views/account/AccountProfileView.vue'
import AccountChangePasswordView from '../views/account/AccountChangePasswordView.vue'
import OrgPositionView from '../views/org/OrgPositionView.vue'
import OrgRankView from '../views/org/OrgRankView.vue'
import OrgContactsView from '../views/org/OrgContactsView.vue'
import OrgChangeLogsView from '../views/org/OrgChangeLogsView.vue'
import PermissionDataScopeView from '../views/permission/PermissionDataScopeView.vue'
import PermissionFieldPermView from '../views/permission/PermissionFieldPermView.vue'
import DelegationListView from '../views/workflow/DelegationListView.vue'
import TwoFactorSetupView from '../views/auth/TwoFactorSetupView.vue'
import WorkflowTemplateDesignerView from '../views/workflow/WorkflowTemplateDesignerView.vue'
import WorkflowTemplateVersionsView from '../views/workflow/WorkflowTemplateVersionsView.vue'
import RuleGroupsView from '../views/rule/RuleGroupsView.vue'
import RuleEditView from '../views/rule/RuleEditView.vue'
import RuleVersionsView from '../views/rule/RuleVersionsView.vue'
import LeaveReportView from '../views/oa/leaves/LeaveReportView.vue'
import TeamCalendarView from '../views/oa/leaves/TeamCalendarView.vue'
import ExpenseFinanceReviewView from '../views/oa/expenses/ExpenseFinanceReviewView.vue'
import ExpenseReportView from '../views/oa/expenses/ExpenseReportView.vue'
import SealLedgerView from '../views/oa/seals/SealLedgerView.vue'
import SealReturnsView from '../views/oa/seals/SealReturnsView.vue'
import PurchaseArrivalView from '../views/oa/purchases/PurchaseArrivalView.vue'
import PurchaseAcceptanceView from '../views/oa/purchases/PurchaseAcceptanceView.vue'
import ContractArchiveView from '../views/contract/ContractArchiveView.vue'
import ContractExpiryView from '../views/contract/ContractExpiryView.vue'
import ContractReportView from '../views/contract/ContractReportView.vue'
import ForceChangePasswordView from '../views/auth/ForceChangePasswordView.vue'

const oaRoutes: RouteRecordRaw[] = [
  { path: 'oa/leaves/create', name: 'leave-create', component: LeaveFormView, meta: { title: '新建请假', module: 'oa' } },
  {
    path: 'oa/leaves/:id/edit',
    name: 'leave-edit',
    component: LeaveFormView,
    meta: { title: '编辑请假', module: 'oa' },
  },
  { path: 'oa/leaves/:id', name: 'leave-detail', component: LeaveDetailView, meta: { title: '请假详情', module: 'oa' } },
  { path: 'oa/leaves/report', name: 'leave-report', component: LeaveReportView, meta: { title: '请假报表', module: 'oa' } },
  { path: 'oa/leaves/team-calendar', name: 'leave-team-calendar', component: TeamCalendarView, meta: { title: '团队请假日历', module: 'oa' } },
  { path: 'oa/leaves', name: 'leave-list', component: LeaveListView, meta: { title: '请假', module: 'oa' } },

  {
    path: 'oa/expenses/create',
    name: 'expense-create',
    component: ExpenseFormView,
    meta: { title: '新建报销', module: 'oa' },
  },
  {
    path: 'oa/expenses/:id/edit',
    name: 'expense-edit',
    component: ExpenseFormView,
    meta: { title: '编辑报销', module: 'oa' },
  },
  {
    path: 'oa/expenses/:id',
    name: 'expense-detail',
    component: ExpenseDetailView,
    meta: { title: '报销详情', module: 'oa' },
  },
  { path: 'oa/expenses/finance-review', name: 'expense-finance-review', component: ExpenseFinanceReviewView, meta: { title: '财务审核', module: 'oa' } },
  { path: 'oa/expenses/report', name: 'expense-report', component: ExpenseReportView, meta: { title: '报销报表', module: 'oa' } },
  { path: 'oa/expenses', name: 'expense-list', component: ExpenseListView, meta: { title: '报销', module: 'oa' } },

  { path: 'oa/seals/create', name: 'seal-create', component: SealFormView, meta: { title: '新建用章', module: 'oa' } },
  { path: 'oa/seals/:id/edit', name: 'seal-edit', component: SealFormView, meta: { title: '编辑用章', module: 'oa' } },
  { path: 'oa/seals/:id', name: 'seal-detail', component: SealDetailView, meta: { title: '用章详情', module: 'oa' } },
  { path: 'oa/seals/ledger', name: 'seal-ledger', component: SealLedgerView, meta: { title: '用章台账', module: 'oa' } },
  { path: 'oa/seals/returns', name: 'seal-returns', component: SealReturnsView, meta: { title: '印章归还', module: 'oa' } },
  { path: 'oa/seals', name: 'seal-list', component: SealListView, meta: { title: '用章申请', module: 'oa' } },

  {
    path: 'oa/purchases/create',
    name: 'purchase-create',
    component: PurchaseFormView,
    meta: { title: '新建采购', module: 'oa' },
  },
  {
    path: 'oa/purchases/:id/edit',
    name: 'purchase-edit',
    component: PurchaseFormView,
    meta: { title: '编辑采购', module: 'oa' },
  },
  {
    path: 'oa/purchases/:id',
    name: 'purchase-detail',
    component: PurchaseDetailView,
    meta: { title: '采购详情', module: 'oa' },
  },
  { path: 'oa/purchases/arrival', name: 'purchase-arrival', component: PurchaseArrivalView, meta: { title: '到货登记', module: 'oa' } },
  { path: 'oa/purchases/acceptance', name: 'purchase-acceptance', component: PurchaseAcceptanceView, meta: { title: '验收管理', module: 'oa' } },
  { path: 'oa/purchases', name: 'purchase-list', component: PurchaseListView, meta: { title: '采购申请', module: 'oa' } },

  {
    path: 'contracts/create',
    name: 'contract-create',
    component: ContractFormView,
    meta: { title: '新建合同', module: 'contract' },
  },
  {
    path: 'contracts/:id/edit',
    name: 'contract-edit',
    component: ContractFormView,
    meta: { title: '编辑合同', module: 'contract' },
  },
  {
    path: 'contracts/:id',
    name: 'contract-detail',
    component: ContractDetailView,
    meta: { title: '合同详情', module: 'contract' },
  },
  { path: 'contracts/archive', name: 'contract-archive', component: ContractArchiveView, meta: { title: '合同归档', module: 'contract' } },
  { path: 'contracts/expiry-reminders', name: 'contract-expiry', component: ContractExpiryView, meta: { title: '到期提醒', module: 'contract' } },
  { path: 'contracts/report', name: 'contract-report', component: ContractReportView, meta: { title: '合同报表', module: 'contract' } },
  { path: 'contracts', name: 'contract-list', component: ContractListView, meta: { title: '合同管理', module: 'contract' } },

  {
    path: 'notices/create',
    name: 'notice-create',
    component: NoticeFormView,
    meta: { title: '新建公告', module: 'notice' },
  },
  {
    path: 'notices/:id/edit',
    name: 'notice-edit',
    component: NoticeFormView,
    meta: { title: '编辑公告', module: 'notice' },
  },
  {
    path: 'notices/:id',
    name: 'notice-detail',
    component: NoticeDetailView,
    meta: { title: '公告详情', module: 'notice' },
  },
  { path: 'notices', name: 'notice-list', component: NoticeListView, meta: { title: '通知公告', module: 'notice' } },
]

const moduleRoutes: RouteRecordRaw[] = [
  ...oaRoutes,
  { path: 'todos/done', name: 'todo-done', component: TodoDoneView, meta: { title: '我的已办', module: 'message' } },
  { path: 'todos', name: 'todo-list', component: TodoListView, meta: { title: '我的待办', module: 'message' } },
  {
    path: 'applications/cc',
    name: 'applications-cc',
    component: CcListView,
    meta: { title: '抄送我的', module: 'workflow' },
  },
  {
    path: 'applications',
    name: 'applications-list',
    component: ApplicationListView,
    meta: { title: '我的申请', module: 'workflow' },
  },
  { path: 'messages', name: 'message-list', component: MessageListView, meta: { title: '消息中心', module: 'message' } },
  { path: 'account/profile', name: 'account-profile', component: AccountProfileView, meta: { title: '个人信息', module: 'org' } },
  { path: 'account/change-password', name: 'account-change-password', component: AccountChangePasswordView, meta: { title: '修改密码', module: 'org' } },
  { path: 'account/2fa-setup', name: 'account-2fa-setup', component: TwoFactorSetupView, meta: { title: '二步验证设置', module: 'org' } },
  { path: 'system', redirect: '/system/configs' },
  { path: 'system/configs', name: 'system-configs', component: SystemConfigsView, meta: { title: '系统参数', module: 'system' } },
  { path: 'system/dicts', name: 'system-dicts', component: SystemDictsView, meta: { title: '字典管理', module: 'system' } },
  { path: 'system/number-rules', name: 'system-number-rules', component: SystemNumberRulesView, meta: { title: '编号规则', module: 'system' } },
  { path: 'system/work-calendar', name: 'system-work-calendar', component: SystemWorkCalendarView, meta: { title: '工作日历', module: 'system' } },
  { path: 'system/import-export', name: 'system-import-export', component: SystemImportExportView, meta: { title: '导入导出', module: 'system' } },
  { path: 'org', redirect: '/org/depts' },
  { path: 'org/depts', name: 'org-depts', component: OrgDeptView, meta: { title: '组织架构', module: 'org' } },
  { path: 'org/users', name: 'org-users', component: OrgUserView, meta: { title: '用户管理', module: 'org' } },
  { path: 'org/positions', name: 'org-positions', component: OrgPositionView, meta: { title: '岗位管理', module: 'org' } },
  { path: 'org/ranks', name: 'org-ranks', component: OrgRankView, meta: { title: '职级管理', module: 'org' } },
  { path: 'org/contacts', name: 'org-contacts', component: OrgContactsView, meta: { title: '通讯录', module: 'org' } },
  { path: 'org/change-logs', name: 'org-change-logs', component: OrgChangeLogsView, meta: { title: '变更日志', module: 'org' } },
  { path: 'permission', redirect: '/permission/roles' },
  { path: 'permission/roles', name: 'permission-roles', component: PermissionRoleView, meta: { title: '角色管理', module: 'permission' } },
  { path: 'permission/menus', name: 'permission-menus', component: PermissionMenuView, meta: { title: '菜单管理', module: 'permission' } },
  { path: 'permission/buttons', name: 'permission-buttons', component: PermissionButtonView, meta: { title: '按钮权限', module: 'permission' } },
  { path: 'permission/temp-auths', name: 'permission-temp-auths', component: PermissionTempAuthView, meta: { title: '临时授权', module: 'permission' } },
  { path: 'permission/preview', name: 'permission-preview', component: PermissionPreviewView, meta: { title: '权限预览', module: 'permission' } },
  { path: 'permission/data-scopes', name: 'permission-data-scopes', component: PermissionDataScopeView, meta: { title: '数据权限', module: 'permission' } },
  { path: 'permission/field-permissions', name: 'permission-field-perms', component: PermissionFieldPermView, meta: { title: '字段权限', module: 'permission' } },
  { path: 'workflow', redirect: '/workflow/templates' },
  { path: 'workflow/templates/:id(\\d+)/designer', name: 'wf-template-designer', component: WorkflowTemplateDesignerView, meta: { title: '模板设计', module: 'workflow' } },
  { path: 'workflow/templates/:id(\\d+)/versions', name: 'wf-template-versions', component: WorkflowTemplateVersionsView, meta: { title: '版本管理', module: 'workflow' } },
  { path: 'workflow/templates', name: 'workflow-templates', component: WorkflowTemplatesView, meta: { title: '流程模板', module: 'workflow' } },
  { path: 'workflow/instances', name: 'workflow-instances', component: WorkflowInstancesView, meta: { title: '流程实例', module: 'workflow' } },
  { path: 'workflow/exceptions', name: 'workflow-exceptions', component: WorkflowExceptionsView, meta: { title: '流程异常', module: 'workflow' } },
  { path: 'workflow/simulator', name: 'workflow-simulator', component: WorkflowSimulatorView, meta: { title: '流程模拟', module: 'workflow' } },
  { path: 'workflow/delegations', name: 'workflow-delegations', component: DelegationListView, meta: { title: '审批委托', module: 'workflow' } },
  { path: 'rules/simulator', name: 'rule-simulator', component: RuleSimulatorView, meta: { title: '规则模拟', module: 'rule' } },
  { path: 'rules/groups', name: 'rule-groups', component: RuleGroupsView, meta: { title: '规则分组', module: 'rule' } },
  { path: 'rules/:id(\\d+)/edit', name: 'rule-edit', component: RuleEditView, meta: { title: '规则编辑', module: 'rule' } },
  { path: 'rules/:id(\\d+)/versions', name: 'rule-versions', component: RuleVersionsView, meta: { title: '规则版本', module: 'rule' } },
  { path: 'rules/:id(\\d+)', name: 'rule-detail', component: RuleDetailView, meta: { title: '规则详情', module: 'rule' } },
  { path: 'rules', name: 'rule-list', component: RuleListView, meta: { title: '规则中心', module: 'rule' } },
  { path: 'forms', redirect: '/forms/templates' },
  { path: 'forms/templates', name: 'form-templates', component: FormTemplatesView, meta: { title: '表单模板', module: 'form' } },
  { path: 'forms/templates/:id(\\d+)/versions', name: 'form-versions', component: FormVersionsView, meta: { title: '表单版本', module: 'form' } },
  { path: 'forms/templates/:id(\\d+)/designer', name: 'form-designer', component: FormDesignerView, meta: { title: '表单设计', module: 'form' } },
  { path: 'forms/field-rules', name: 'form-field-rules', component: FormFieldRulesView, meta: { title: '字段权限', module: 'form' } },
  { path: 'meetings', redirect: '/meetings/rooms' },
  {
    path: 'meetings/rooms',
    name: 'meeting-rooms',
    component: MeetingRoomListView,
    meta: { title: '会议室', module: 'meeting' },
  },
  {
    path: 'meetings/bookings',
    name: 'meeting-bookings',
    component: MeetingBookingListView,
    meta: { title: '会议预约', module: 'meeting' },
  },
  {
    path: 'assets/:id(\\d+)',
    name: 'asset-detail',
    component: AssetDetailView,
    meta: { title: '资产详情', module: 'asset' },
  },
  { path: 'assets', name: 'asset-list', component: AssetListView, meta: { title: '固定资产', module: 'asset' } },
  {
    path: 'supplies/records',
    name: 'supply-records',
    component: SupplyRecordsView,
    meta: { title: '出入库记录', module: 'asset' },
  },
  { path: 'supplies', name: 'supply-list', component: SupplyListView, meta: { title: '办公用品', module: 'asset' } },
  { path: 'files/recycle-bin', name: 'file-recycle-bin', component: FileRecycleBinView, meta: { title: '回收站', module: 'file' } },
  { path: 'files/:id(\\d+)', name: 'file-detail', component: FileDetailView, meta: { title: '文件详情', module: 'file' } },
  { path: 'files', name: 'file-library', component: FileLibraryView, meta: { title: '文件资料库', module: 'file' } },
  { path: 'reports', redirect: '/reports/workflow-efficiency' },
  { path: 'reports/workflow-efficiency', name: 'report-workflow-efficiency', component: ReportWorkflowEfficiencyView, meta: { title: '流程效率', module: 'report' } },
  { path: 'reports/todos', name: 'report-todos', component: ReportTodosView, meta: { title: '待办统计', module: 'report' } },
  { path: 'reports/leaves', name: 'report-leaves', component: ReportLeavesView, meta: { title: '请假统计', module: 'report' } },
  { path: 'reports/expenses', name: 'report-expenses', component: ReportExpensesView, meta: { title: '报销统计', module: 'report' } },
  { path: 'reports/contracts', name: 'report-contracts', component: ReportContractsView, meta: { title: '合同统计', module: 'report' } },
  { path: 'reports/assets', name: 'report-assets', component: ReportAssetsView, meta: { title: '资产统计', module: 'report' } },
  { path: 'reports/users', name: 'report-users', component: ReportUsersView, meta: { title: '用户统计', module: 'report' } },
  { path: 'audit', redirect: '/audit/login-logs' },
  { path: 'audit/login-logs', name: 'audit-login-logs', component: AuditLoginLogsView, meta: { title: '登录日志', module: 'audit' } },
  { path: 'audit/operation-logs', name: 'audit-operation-logs', component: AuditOperationLogsView, meta: { title: '操作日志', module: 'audit' } },
  { path: 'audit/permission-logs', name: 'audit-permission-logs', component: AuditPermissionLogsView, meta: { title: '权限变更日志', module: 'audit' } },
  { path: 'audit/rule-logs', name: 'audit-rule-logs', component: AuditRuleLogsView, meta: { title: '规则变更日志', module: 'audit' } },
  { path: 'audit/file-download-logs', name: 'audit-file-download-logs', component: AuditFileDownloadLogsView, meta: { title: '文件下载日志', module: 'audit' } },
  { path: 'ops', redirect: '/ops/health' },
  { path: 'ops/health', name: 'ops-health', component: OpsHealthView, meta: { title: '运维监控', module: 'ops' } },
  { path: 'ops/online-users', name: 'ops-online-users', component: OpsOnlineUsersView, meta: { title: '在线用户', module: 'ops' } },
  { path: 'ops/job-logs', name: 'ops-job-logs', component: OpsJobLogsView, meta: { title: '任务日志', module: 'ops' } },
  { path: 'ops/exceptions', name: 'ops-exceptions', component: OpsExceptionsView, meta: { title: '异常中心', module: 'ops' } },
  { path: 'ops/backups', name: 'ops-backups', component: OpsBackupsView, meta: { title: '备份记录', module: 'ops' } },
]

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/force-change-password', name: 'force-change-password', component: ForceChangePasswordView },
    { path: '/2fa/setup', name: '2fa-setup', component: TwoFactorSetupView },
    {
      path: '/',
      component: AppShell,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: DashboardView, meta: { title: '工作台', module: 'dashboard' } },
        ...moduleRoutes,
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (to.path === '/login' && authStore.isAuthenticated) {
    return '/dashboard'
  }
  if (to.path === '/login') {
    return true
  }
  if (to.path === '/force-change-password') {
    if (!authStore.isAuthenticated) {
      return '/login'
    }
    return true
  }
  if (to.path === '/2fa/setup') {
    if (!authStore.isAuthenticated) return '/login'
    return true
  }
  if (!authStore.isAuthenticated) {
    return '/login'
  }
  if (authStore.token && !authStore.user) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.signOut()
      return '/login'
    }
  }
  if (authStore.passwordExpired) {
    return '/force-change-password'
  }
  if (authStore.token && authStore.menus.length === 0) {
    try {
      await authStore.loadMenus()
    } catch {
      /* 侧栏将仅保留工作台兜底 */
    }
  }
  if (!authStore.isRouteAllowed(to.path)) {
    return '/dashboard'
  }
  return true
})
