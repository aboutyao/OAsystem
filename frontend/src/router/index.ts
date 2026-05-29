import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppShell from '../layouts/AppShell.vue'

// 懒加载 - 只在访问时才加载
const LoginView = () => import('../views/login/LoginView.vue')
const DashboardView = () => import('../views/dashboard/DashboardView.vue')
const ForceChangePasswordView = () => import('../views/auth/ForceChangePasswordView.vue')
const TwoFactorSetupView = () => import('../views/auth/TwoFactorSetupView.vue')

// OA 模块
const LeaveListView = () => import('../views/oa/leaves/LeaveListView.vue')
const LeaveDetailView = () => import('../views/oa/leaves/LeaveDetailView.vue')
const LeaveFormView = () => import('../views/oa/leaves/LeaveFormView.vue')
const LeaveReportView = () => import('../views/oa/leaves/LeaveReportView.vue')
const TeamCalendarView = () => import('../views/oa/leaves/TeamCalendarView.vue')

const ExpenseListView = () => import('../views/oa/expenses/ExpenseListView.vue')
const ExpenseDetailView = () => import('../views/oa/expenses/ExpenseDetailView.vue')
const ExpenseFormView = () => import('../views/oa/expenses/ExpenseFormView.vue')
const ExpenseFinanceReviewView = () => import('../views/oa/expenses/ExpenseFinanceReviewView.vue')
const ExpenseReportView = () => import('../views/oa/expenses/ExpenseReportView.vue')

const SealListView = () => import('../views/oa/seals/SealListView.vue')
const SealDetailView = () => import('../views/oa/seals/SealDetailView.vue')
const SealFormView = () => import('../views/oa/seals/SealFormView.vue')
const SealLedgerView = () => import('../views/oa/seals/SealLedgerView.vue')
const SealReturnsView = () => import('../views/oa/seals/SealReturnsView.vue')

const PurchaseListView = () => import('../views/oa/purchases/PurchaseListView.vue')
const PurchaseDetailView = () => import('../views/oa/purchases/PurchaseDetailView.vue')
const PurchaseFormView = () => import('../views/oa/purchases/PurchaseFormView.vue')
const PurchaseArrivalView = () => import('../views/oa/purchases/PurchaseArrivalView.vue')
const PurchaseAcceptanceView = () => import('../views/oa/purchases/PurchaseAcceptanceView.vue')

// 合同模块
const ContractListView = () => import('../views/contract/ContractListView.vue')
const ContractDetailView = () => import('../views/contract/ContractDetailView.vue')
const ContractFormView = () => import('../views/contract/ContractFormView.vue')
const ContractArchiveView = () => import('../views/contract/ContractArchiveView.vue')
const ContractExpiryView = () => import('../views/contract/ContractExpiryView.vue')
const ContractReportView = () => import('../views/contract/ContractReportView.vue')

// 公告模块
const NoticeListView = () => import('../views/notice/NoticeListView.vue')
const NoticeDetailView = () => import('../views/notice/NoticeDetailView.vue')
const NoticeFormView = () => import('../views/notice/NoticeFormView.vue')

// 工作流模块
const TodoListView = () => import('../views/workflow/TodoListView.vue')
const TodoDoneView = () => import('../views/workflow/TodoDoneView.vue')
const ApplicationListView = () => import('../views/workflow/ApplicationListView.vue')
const CcListView = () => import('../views/workflow/CcListView.vue')
const WorkflowTemplatesView = () => import('../views/workflow/WorkflowTemplatesView.vue')
const WorkflowInstancesView = () => import('../views/workflow/WorkflowInstancesView.vue')
const WorkflowExceptionsView = () => import('../views/workflow/WorkflowExceptionsView.vue')
const WorkflowSimulatorView = () => import('../views/workflow/WorkflowSimulatorView.vue')
const DelegationListView = () => import('../views/workflow/DelegationListView.vue')
const WorkflowTemplateDesignerView = () => import('../views/workflow/WorkflowTemplateDesignerView.vue')
const WorkflowTemplateVersionsView = () => import('../views/workflow/WorkflowTemplateVersionsView.vue')

// 消息模块
const MessageListView = () => import('../views/message/MessageListView.vue')
const NotificationSettingsView = () => import('../views/message/NotificationSettingsView.vue')

// 系统模块
const SystemConfigsView = () => import('../views/system/SystemConfigsView.vue')
const SystemDictsView = () => import('../views/system/SystemDictsView.vue')
const SystemNumberRulesView = () => import('../views/system/SystemNumberRulesView.vue')
const SystemWorkCalendarView = () => import('../views/system/SystemWorkCalendarView.vue')
const SystemImportExportView = () => import('../views/system/SystemImportExportView.vue')

// 表单模块
const FormTemplatesView = () => import('../views/form/FormTemplatesView.vue')
const FormVersionsView = () => import('../views/form/FormVersionsView.vue')
const FormDesignerView = () => import('../views/form/FormDesignerView.vue')
const FormFieldRulesView = () => import('../views/form/FormFieldRulesView.vue')

// 报表模块
const ReportWorkflowEfficiencyView = () => import('../views/report/ReportWorkflowEfficiencyView.vue')
const ReportTodosView = () => import('../views/report/ReportTodosView.vue')
const ReportLeavesView = () => import('../views/report/ReportLeavesView.vue')
const ReportExpensesView = () => import('../views/report/ReportExpensesView.vue')
const ReportContractsView = () => import('../views/report/ReportContractsView.vue')
const ReportAssetsView = () => import('../views/report/ReportAssetsView.vue')
const ReportUsersView = () => import('../views/report/ReportUsersView.vue')

// 组织模块
const OrgDeptView = () => import('../views/org/OrgDeptView.vue')
const OrgUserView = () => import('../views/org/OrgUserView.vue')
const OrgPositionView = () => import('../views/org/OrgPositionView.vue')
const OrgRankView = () => import('../views/org/OrgRankView.vue')
const OrgContactsView = () => import('../views/org/OrgContactsView.vue')
const OrgChangeLogsView = () => import('../views/org/OrgChangeLogsView.vue')

// 权限模块
const PermissionRoleView = () => import('../views/permission/PermissionRoleView.vue')
const PermissionMenuView = () => import('../views/permission/PermissionMenuView.vue')
const PermissionButtonView = () => import('../views/permission/PermissionButtonView.vue')
const PermissionTempAuthView = () => import('../views/permission/PermissionTempAuthView.vue')
const PermissionPreviewView = () => import('../views/permission/PermissionPreviewView.vue')
const PermissionDataScopeView = () => import('../views/permission/PermissionDataScopeView.vue')
const PermissionFieldPermView = () => import('../views/permission/PermissionFieldPermView.vue')

// 规则模块
const RuleListView = () => import('../views/rule/RuleListView.vue')
const RuleDetailView = () => import('../views/rule/RuleDetailView.vue')
const RuleSimulatorView = () => import('../views/rule/RuleSimulatorView.vue')
const RuleGroupsView = () => import('../views/rule/RuleGroupsView.vue')
const RuleEditView = () => import('../views/rule/RuleEditView.vue')
const RuleVersionsView = () => import('../views/rule/RuleVersionsView.vue')

// 审计模块
const AuditLoginLogsView = () => import('../views/audit/AuditLoginLogsView.vue')
const AuditOperationLogsView = () => import('../views/audit/AuditOperationLogsView.vue')
const AuditPermissionLogsView = () => import('../views/audit/AuditPermissionLogsView.vue')
const AuditRuleLogsView = () => import('../views/audit/AuditRuleLogsView.vue')
const AuditFileDownloadLogsView = () => import('../views/audit/AuditFileDownloadLogsView.vue')

// 文件模块
const FileLibraryView = () => import('../views/file/FileLibraryView.vue')
const FileDetailView = () => import('../views/file/FileDetailView.vue')
const FileRecycleBinView = () => import('../views/file/FileRecycleBinView.vue')

// 运维模块
const OpsHealthView = () => import('../views/ops/OpsHealthView.vue')
const OpsOnlineUsersView = () => import('../views/ops/OpsOnlineUsersView.vue')
const OpsJobLogsView = () => import('../views/ops/OpsJobLogsView.vue')
const OpsExceptionsView = () => import('../views/ops/OpsExceptionsView.vue')
const OpsBackupsView = () => import('../views/ops/OpsBackupsView.vue')

// 账户模块
const AccountProfileView = () => import('../views/account/AccountProfileView.vue')
const AccountChangePasswordView = () => import('../views/account/AccountChangePasswordView.vue')

// 会议模块
const MeetingRoomListView = () => import('../views/meeting/MeetingRoomListView.vue')
const MeetingBookingListView = () => import('../views/meeting/MeetingBookingListView.vue')

// 资产模块
const AssetListView = () => import('../views/asset/AssetListView.vue')
const AssetDetailView = () => import('../views/asset/AssetDetailView.vue')

// 用品模块
const SupplyListView = () => import('../views/supply/SupplyListView.vue')
const SupplyRecordsView = () => import('../views/supply/SupplyRecordsView.vue')

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
  { path: 'messages/settings', name: 'notification-settings', component: NotificationSettingsView, meta: { title: '消息设置', module: 'message' } },
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

  // ==================== 智能功能模块 ====================
  { path: 'smart/approval', name: 'smart-approval', component: () => import('../components/SmartApprovalPanel.vue'), meta: { title: '智能审批', module: 'smart' } },
  { path: 'smart/anomaly', name: 'smart-anomaly', component: () => import('../components/AnomalyAlert.vue'), meta: { title: '异常检测', module: 'smart' } },
  { path: 'smart/workload', name: 'smart-workload', component: () => import('../components/WorkloadAnalysis.vue'), meta: { title: '工作负荷', module: 'smart' } },
  { path: 'smart/health', name: 'smart-health', component: () => import('../components/DepartmentHealthDashboard.vue'), meta: { title: '部门健康度', module: 'smart' } },
  { path: 'smart/cost', name: 'smart-cost', component: () => import('../components/CostPredictionChart.vue'), meta: { title: '成本预测', module: 'smart' } },
  { path: 'smart/reminder', name: 'smart-reminder', component: () => import('../components/SmartReminderPanel.vue'), meta: { title: '智能催办', module: 'smart' } },
  { path: 'smart/schedule', name: 'smart-schedule', component: () => import('../components/SmartScheduler.vue'), meta: { title: '智能日程', module: 'smart' } },
  { path: 'smart/knowledge', name: 'smart-knowledge', component: () => import('../components/KnowledgeGraphViewer.vue'), meta: { title: '知识图谱', module: 'smart' } },
  { path: 'smart/contract-risk', name: 'smart-contract-risk', component: () => import('../components/ContractRiskAlert.vue'), meta: { title: '合同风险', module: 'smart' } },
  { path: 'smart/template-market', name: 'smart-template-market', component: () => import('../components/TemplateMarket.vue'), meta: { title: '模板市场', module: 'smart' } },
  { path: 'smart/plugins', name: 'smart-plugins', component: () => import('../components/PluginManager.vue'), meta: { title: '插件管理', module: 'smart' } },
  { path: 'smart/webhooks', name: 'smart-webhooks', component: () => import('../components/WebhookManager.vue'), meta: { title: 'Webhook管理', module: 'smart' } },
  { path: 'smart/scheduler', name: 'smart-scheduler', component: () => import('../components/ScheduledTaskManager.vue'), meta: { title: '定时任务', module: 'smart' } },
  { path: 'smart/open-api', name: 'smart-open-api', component: () => import('../components/OpenApiManager.vue'), meta: { title: 'API开放平台', module: 'smart' } },
  { path: 'smart/theme', name: 'smart-theme', component: () => import('../components/ThemeCustomizer.vue'), meta: { title: '主题定制', module: 'smart' } },
  { path: 'smart/dashboard-config', name: 'smart-dashboard-config', component: () => import('../components/PersonalDashboardConfig.vue'), meta: { title: '仪表盘配置', module: 'smart' } },
  { path: 'smart/accessibility', name: 'smart-accessibility', component: () => import('../components/AccessibilitySettings.vue'), meta: { title: '无障碍设置', module: 'smart' } },
  { path: 'smart/notifications', name: 'smart-notifications', component: () => import('../components/NotificationSettings.vue'), meta: { title: '通知设置', module: 'smart' } },
  { path: 'smart/operation-replay', name: 'smart-operation-replay', component: () => import('../components/OperationReplay.vue'), meta: { title: '操作回放', module: 'smart' } },
  { path: 'smart/data-lifecycle', name: 'smart-data-lifecycle', component: () => import('../components/DataLifecycleStats.vue'), meta: { title: '数据统计', module: 'smart' } },
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
