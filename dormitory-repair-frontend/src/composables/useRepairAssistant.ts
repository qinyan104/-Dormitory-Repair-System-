import { computed, ref, type Ref } from 'vue'
import { aiClassifyApi, aiRepairChatApi } from '../api/ai'
import type { AiClassifyResponse, RepairCategory, RepairChatResponse, UserInfo } from '../types/models'

export interface GuidedRepairForm {
  title: string
  categoryId: string
  dormitoryBuilding: string
  roomNo: string
  description: string
  images: string[]
}

type AssistantPhase = 'problem' | 'detail' | 'impact' | 'extra' | 'review'
type AssistantRole = 'assistant' | 'student'
type ChoiceAction = 'starter' | 'detail' | 'impact' | 'extra' | 'finalize' | 'restart'

interface AssistantMessage {
  id: string
  role: AssistantRole
  text: string
}

interface AssistantChoice {
  label: string
  action: ChoiceAction
  value?: string
  hint?: string
}

interface IssueProfile {
  type: string
  name: string
  keywords: string[]
  categoryKeywords: string[]
  detailOptions: string[]
}

interface RepairAssistantOptions {
  form: Ref<GuidedRepairForm>
  categories: Ref<RepairCategory[]>
  getUser: () => UserInfo | null | undefined
  setAiResult: (result: AiClassifyResponse | null) => void
}

const issueProfiles: IssueProfile[] = [
  {
    type: 'air-conditioner',
    name: '空调',
    keywords: ['空调', '制冷', '制热', '冷气', '暖气', '遥控器'],
    categoryKeywords: ['空调', '电器', '设备'],
    detailOptions: ['不制冷', '不制热', '无法通电或不开机', '漏水', '异响或异味', '遥控器或面板失灵', '其他损坏']
  },
  {
    type: 'water',
    name: '水电/给排水',
    keywords: ['水龙头', '漏水', '下水', '堵', '马桶', '洗手池', '花洒', '水管'],
    categoryKeywords: ['水', '管道', '卫生间', '给排水'],
    detailOptions: ['持续漏水', '下水堵塞', '没有热水', '水压异常', '洁具损坏', '其他给排水问题']
  },
  {
    type: 'electric',
    name: '电路',
    keywords: ['电', '灯', '插座', '跳闸', '断电', '开关', '短路'],
    categoryKeywords: ['电', '照明', '线路'],
    detailOptions: ['宿舍断电', '灯具不亮', '插座无电', '频繁跳闸', '开关损坏', '疑似漏电或短路']
  },
  {
    type: 'network',
    name: '网络',
    keywords: ['网', '网络', '宽带', 'wifi', 'WiFi', '路由器', '校园网'],
    categoryKeywords: ['网络', '网线', '信息'],
    detailOptions: ['无法联网', '网速很慢', '频繁断网', '网口损坏', '路由器异常', '其他网络问题']
  },
  {
    type: 'door-window',
    name: '门窗',
    keywords: ['门', '锁', '钥匙', '窗', '玻璃', '纱窗'],
    categoryKeywords: ['门', '窗', '锁'],
    detailOptions: ['门锁打不开', '门锁松动或损坏', '窗户无法开关', '玻璃破损', '纱窗损坏', '其他门窗问题']
  },
  {
    type: 'furniture',
    name: '家具',
    keywords: ['床', '桌', '椅', '柜', '梯子', '抽屉'],
    categoryKeywords: ['家具', '床', '桌', '椅', '柜'],
    detailOptions: ['床架松动', '桌椅损坏', '柜门或抽屉损坏', '扶梯损坏', '存在尖锐破损', '其他家具问题']
  },
  {
    type: 'other',
    name: '宿舍设施',
    keywords: [],
    categoryKeywords: ['维修', '设施', '其他'],
    detailOptions: ['无法正常使用', '部件破损', '存在安全隐患', '影响日常生活', '需要现场检查', '其他情况']
  }
]

const impactOptions = ['完全无法使用', '可以勉强使用', '偶发问题', '影响同寝室多人', '存在漏水/漏电等安全风险']

const starterChoices: AssistantChoice[] = [
  { label: '空调坏了', action: 'starter', value: '空调坏了', hint: '制冷、制热、漏水等' },
  { label: '水龙头漏水', action: 'starter', value: '水龙头漏水', hint: '漏水、堵塞、热水问题' },
  { label: '宿舍断电', action: 'starter', value: '宿舍断电', hint: '灯、插座、跳闸' },
  { label: '门锁打不开', action: 'starter', value: '门锁打不开', hint: '门窗、钥匙、玻璃' },
  { label: '网络断了', action: 'starter', value: '网络断了', hint: '断网、网速慢、网口' },
  { label: '其他问题', action: 'starter', value: '宿舍设施需要维修', hint: '不确定分类也可以' }
]

const casualOnlyTexts = new Set([
  '你好',
  '您好',
  '你好啊',
  '您好啊',
  '在吗',
  '在不在',
  'hi',
  'hello',
  '嗨',
  '哈喽',
  '谢谢',
  '谢谢你',
  '好的',
  '好',
  'ok',
  '嗯',
  '测试'
])

const repairSignalWords = [
  ...issueProfiles.flatMap(profile => profile.keywords),
  '坏',
  '坏了',
  '报修',
  '维修',
  '故障',
  '不能用',
  '无法使用',
  '无法正常使用',
  '失灵',
  '损坏',
  '漏',
  '堵',
  '断',
  '不亮',
  '不通电',
  '不开机',
  '打不开',
  '不制冷',
  '不制热',
  '异响',
  '异味',
  '跳闸',
  '短路',
  '破损',
  '松动',
  '掉了',
  '卡住',
  '没电',
  '没有热水',
  '水压',
  '网速',
  '断网'
].filter(Boolean)

const normalizeText = (text: string) => text.trim().toLowerCase().replace(/[\s，。！？!?,.、]/g, '')

const isCasualOnlyText = (text: string) => casualOnlyTexts.has(normalizeText(text))

const hasRepairIntent = (text: string) => {
  const normalized = normalizeText(text)
  if (!normalized || isCasualOnlyText(text)) return false
  return repairSignalWords.some(word => normalized.includes(normalizeText(word)))
}

const assistantStepMeta: Array<{ key: AssistantPhase; label: string }> = [
  { key: 'problem', label: '说明问题' },
  { key: 'detail', label: '确认现象' },
  { key: 'impact', label: '影响程度' },
  { key: 'extra', label: '补充信息' },
  { key: 'review', label: '检查提交' }
]

const questionMeta: Record<AssistantPhase, { title: string; description: string; choiceHeading: string }> = {
  problem: {
    title: '先告诉我哪里出了问题',
    description: '可以直接输入一句话，也可以点一个常见问题开始。',
    choiceHeading: '常见报修'
  },
  detail: {
    title: '选择最接近的具体现象',
    description: '只选最接近的一项即可，后面还能补充说明。',
    choiceHeading: '具体现象'
  },
  impact: {
    title: '确认影响程度',
    description: '这会帮助管理员和维修人员判断处理优先级。',
    choiceHeading: '影响程度'
  },
  extra: {
    title: '还有补充吗',
    description: '比如持续多久、是否有异响异味、是否已经拍照。',
    choiceHeading: '补充方式'
  },
  review: {
    title: '检查报修摘要',
    description: '确认无误后提交，或继续补充纠正。',
    choiceHeading: '下一步'
  }
}

const makeMessageId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`

export function useRepairAssistant(options: RepairAssistantOptions) {
  const { form, categories, getUser, setAiResult } = options

  const phase = ref<AssistantPhase>('problem')
  const messages = ref<AssistantMessage[]>([])
  const choices = ref<AssistantChoice[]>([])
  const input = ref('')
  const summary = ref('')
  const classificationLoading = ref(false)
  const classificationError = ref('')
  const chatLoading = ref(false)
  const chatError = ref('')

  const draft = ref({
    problem: '',
    issueType: 'other',
    issueDetail: '',
    impact: '',
    extra: ''
  })

  const inputPlaceholder = computed(() => {
    if (phase.value === 'problem') return '例如：空调坏了，或者水龙头一直漏水'
    if (phase.value === 'detail') return '也可以自己输入更准确的故障情况'
    if (phase.value === 'impact') return '也可以自己输入影响程度'
    if (phase.value === 'extra') return '补充持续时间、异常声音、是否影响使用等'
    return '输入要补充或纠正的内容'
  })

  const activeStepIndex = computed(() => Math.max(0, assistantStepMeta.findIndex(step => step.key === phase.value)))

  const progressPercent = computed(() => {
    if (assistantStepMeta.length <= 1) return 100
    return Math.round((activeStepIndex.value / (assistantStepMeta.length - 1)) * 100)
  })

  const assistantSteps = computed(() => assistantStepMeta.map((step, index) => ({
    ...step,
    state: index < activeStepIndex.value ? 'done' : index === activeStepIndex.value ? 'current' : 'pending'
  })))

  const activeQuestion = computed(() => questionMeta[phase.value])

  const choiceHeading = computed(() => questionMeta[phase.value].choiceHeading)

  const locationText = computed(() => getCurrentUserLocation() || '待补充宿舍位置')

  const collectedItems = computed(() => [
    { label: '宿舍位置', value: locationText.value, complete: !!getCurrentUserLocation() },
    { label: '问题概述', value: draft.value.problem || '等待说明', complete: !!draft.value.problem },
    { label: '具体现象', value: draft.value.issueDetail || '等待选择', complete: !!draft.value.issueDetail },
    { label: '影响程度', value: draft.value.impact || '等待确认', complete: !!draft.value.impact }
  ])

  const addMessage = (role: AssistantRole, text: string) => {
    messages.value.push({ id: makeMessageId(), role, text })
  }

  const getCurrentUserLocation = () => {
    const user = getUser()
    const building = form.value.dormitoryBuilding || user?.dormitoryBuilding || ''
    const room = form.value.roomNo || user?.roomNo || ''
    if (building && room) return `${building} ${room}`
    if (building) return building
    if (room) return room
    return ''
  }

  const fillLocationFromUser = () => {
    const user = getUser()
    if (!form.value.dormitoryBuilding && user?.dormitoryBuilding) {
      form.value.dormitoryBuilding = user.dormitoryBuilding
    }
    if (!form.value.roomNo && user?.roomNo) {
      form.value.roomNo = user.roomNo
    }
  }

  const findIssueProfile = (type: string) => issueProfiles.find(item => item.type === type) || issueProfiles[issueProfiles.length - 1]

  const inferIssueProfile = (text: string) => {
    const normalized = text.toLowerCase()
    return issueProfiles.find(profile =>
      profile.keywords.some(keyword => normalized.includes(keyword.toLowerCase()))
    ) || issueProfiles[issueProfiles.length - 1]
  }

  const buildDetailChoices = (profile: IssueProfile) => {
    choices.value = profile.detailOptions.map(label => ({ label, action: 'detail', value: label }))
  }

  const askImpact = () => {
    phase.value = 'impact'
    choices.value = impactOptions.map(label => ({ label, action: 'impact', value: label }))
    addMessage('assistant', '再确认一下影响程度，选最接近的一项就好。维修人员会根据这个判断处理优先级。')
  }

  const askExtra = () => {
    phase.value = 'extra'
    choices.value = [
      { label: '没有补充，生成摘要', action: 'finalize' },
      { label: '我再补充一句', action: 'extra' }
    ]
    addMessage('assistant', '最后再看一下有没有补充：持续多久、是否有异响异味、是否已经拍照，都可以在这里说明。')
  }

  const guessCategoryId = () => {
    const profile = findIssueProfile(draft.value.issueType)
    const keywords = [profile.name, draft.value.issueDetail, ...profile.categoryKeywords].filter(Boolean)
    const matched = categories.value.find(category => {
      const searchable = `${category.categoryName || ''} ${category.description || ''}`
      return keywords.some(keyword => searchable.includes(keyword))
    })
    return matched?.id ? String(matched.id) : ''
  }

  const buildTitle = () => {
    const profile = findIssueProfile(draft.value.issueType)
    if (draft.value.issueDetail) return `${profile.name} - ${draft.value.issueDetail}`
    return `${profile.name}报修`
  }

  const buildSummary = () => {
    const location = getCurrentUserLocation()
    const lines = [
      `宿舍位置：${location || '待补充'}`,
      `问题概述：${draft.value.problem || '待补充'}`,
      `具体情况：${draft.value.issueDetail || '待补充'}`,
      `影响程度：${draft.value.impact || '待补充'}`
    ]

    if (draft.value.extra) {
      lines.push(`补充说明：${draft.value.extra}`)
    }

    lines.push('请维修人员到场检查并处理。')
    return lines.join('\n')
  }

  const classifyDraft = async () => {
    classificationLoading.value = true
    classificationError.value = ''
    setAiResult(null)

    try {
      const data: any = await aiClassifyApi({
        title: form.value.title,
        description: form.value.description
      })

      if (data) {
        setAiResult(data)
        if (data.categoryId) {
          form.value.categoryId = String(data.categoryId)
        }
        const categoryName = data.categoryName || data.category || '对应分类'
        addMessage('assistant', `我已根据摘要辅助判断分类为“${categoryName}”。提交前你仍然可以手动调整。`)
      } else {
        classificationError.value = 'AI 分类暂未返回结果，请在手动表单中确认分类。'
      }
    } catch {
      classificationError.value = 'AI 分类暂不可用，摘要已生成，请在手动表单中确认分类。'
    } finally {
      if (!form.value.categoryId) {
        form.value.categoryId = guessCategoryId()
      }
      classificationLoading.value = false
    }
  }

  const mapAiChoices = (response: RepairChatResponse): AssistantChoice[] => {
    return (response.choices || [])
      .filter(choice => ['detail', 'impact', 'extra', 'finalize'].includes(choice.action))
      .slice(0, 5)
      .map(choice => ({
        label: choice.label,
        value: choice.value || choice.label,
        action: choice.action,
        hint: choice.hint
      }))
  }

  const inferNextPhaseFromChoices = (mappedChoices: AssistantChoice[]): AssistantPhase => {
    const firstAction = mappedChoices.find(choice => choice.action !== 'finalize')?.action
    if (firstAction === 'detail') return 'detail'
    if (firstAction === 'impact') return 'impact'
    if (firstAction === 'extra') return 'extra'
    if (!draft.value.issueDetail) return 'detail'
    if (!draft.value.impact) return 'impact'
    return 'extra'
  }

  const setLocalChoicesForCurrentPhase = () => {
    if (phase.value === 'problem') {
      choices.value = starterChoices.map(choice => ({ ...choice }))
      return
    }
    if (phase.value === 'detail') {
      buildDetailChoices(findIssueProfile(draft.value.issueType))
      return
    }
    if (phase.value === 'impact') {
      choices.value = impactOptions.map(label => ({ label, action: 'impact', value: label }))
      return
    }
    if (phase.value === 'extra') {
      choices.value = [
        { label: '没有补充，生成摘要', action: 'finalize' },
        { label: '我再补充一句', action: 'extra' }
      ]
    }
  }

  const applyAiRepairChat = async (text: string, response: RepairChatResponse) => {
    const reply = response.reply?.trim()

    if (response.repairIntent === false) {
      phase.value = 'problem'
      choices.value = starterChoices.map(choice => ({ ...choice }))
      addMessage('assistant', reply || '你好，我在。请告诉我具体哪里需要报修，也可以直接点下面的常见报修。')
      return true
    }

    if (phase.value === 'problem' && !draft.value.problem) {
      draft.value.problem = text
    }

    if (response.issueType) {
      draft.value.issueType = response.issueType
    } else if (phase.value === 'problem') {
      draft.value.issueType = inferIssueProfile(text).type
    }

    if (response.issueDetail) {
      draft.value.issueDetail = response.issueDetail
    }
    if (response.impact) {
      draft.value.impact = response.impact
    }
    if (response.extra) {
      draft.value.extra = response.extra
    }

    const mappedChoices = mapAiChoices(response)

    const hasEnoughRepairDetail = !!draft.value.issueDetail && !!draft.value.impact
    if (response.readyToSummarize && hasRepairIntent(draft.value.problem) && hasEnoughRepairDetail) {
      fillLocationFromUser()
      form.value.title = buildTitle()
      summary.value = response.summary?.trim() || buildSummary()
      form.value.description = summary.value
      if (!form.value.categoryId) {
        form.value.categoryId = guessCategoryId()
      }
      phase.value = 'review'
      choices.value = mappedChoices.length ? mappedChoices : [
        { label: '继续补充', action: 'extra' },
        { label: '重新开始', action: 'restart' }
      ]
      addMessage('assistant', reply || '我已经整理好报修摘要，请检查是否准确。')
      await classifyDraft()
      return true
    }

    phase.value = inferNextPhaseFromChoices(mappedChoices)
    choices.value = mappedChoices
    if (!choices.value.length) {
      setLocalChoicesForCurrentPhase()
    }

    addMessage('assistant', reply || '我继续帮你补全报修信息，请选择最接近的情况。')
    return true
  }

  const tryAiRepairChat = async (text: string) => {
    chatLoading.value = true
    chatError.value = ''
    try {
      fillLocationFromUser()
      const data: any = await aiRepairChatApi({
        message: text,
        phase: phase.value,
        dormitoryBuilding: form.value.dormitoryBuilding,
        roomNo: form.value.roomNo,
        problem: draft.value.problem,
        issueType: draft.value.issueType,
        issueDetail: draft.value.issueDetail,
        impact: draft.value.impact,
        extra: draft.value.extra
      })
      if (!data) return false
      return await applyAiRepairChat(text, data)
    } catch {
      chatError.value = 'AI 对话暂不可用，已切换为本地引导。'
      return false
    } finally {
      chatLoading.value = false
    }
  }

  const finalizeAssistantDraft = async () => {
    if (!hasRepairIntent(draft.value.problem)) {
      phase.value = 'problem'
      choices.value = starterChoices.map(choice => ({ ...choice }))
      addMessage('assistant', '我还没有采集到明确的报修问题，所以先不生成摘要。请告诉我具体哪里坏了，或直接点下面的常见报修。')
      return
    }

    fillLocationFromUser()
    form.value.title = buildTitle()
    summary.value = buildSummary()
    form.value.description = summary.value
    if (!form.value.categoryId) {
      form.value.categoryId = guessCategoryId()
    }

    phase.value = 'review'
    choices.value = [
      { label: '继续补充', action: 'extra' },
      { label: '重新开始', action: 'restart' }
    ]
    addMessage('assistant', '我先整理成下面这份报修摘要。请检查是否准确，必要时可以直接修改摘要或继续补充。')
    await classifyDraft()
  }

  const processFreeText = async (text: string) => {
    if (phase.value !== 'review') {
      const handledByAi = await tryAiRepairChat(text)
      if (handledByAi) return
    }

    if (phase.value === 'problem') {
      if (!hasRepairIntent(text)) {
        choices.value = starterChoices.map(choice => ({ ...choice }))
        const location = getCurrentUserLocation()
        if (isCasualOnlyText(text)) {
          const locationHint = location ? `需要报修时，我会带上你的宿舍位置（${location}）。` : ''
          addMessage('assistant', `你好，我在。${locationHint}先告诉我具体哪里出了问题，比如“空调不制冷”“水龙头漏水”“门锁打不开”，也可以直接点下面的常见报修。`)
        } else {
          addMessage('assistant', '这句话里还没有明确的报修对象。我需要先知道哪里坏了，比如“空调不制冷”“水龙头漏水”“门锁打不开”，也可以直接点下面的常见报修。')
        }
        return
      }

      const profile = inferIssueProfile(text)
      draft.value.problem = text
      draft.value.issueType = profile.type
      phase.value = 'detail'
      buildDetailChoices(profile)

      const location = getCurrentUserLocation()
      const locationSentence = location ? `我读取到你的宿舍位置是 ${location}。` : '我还没有读取到完整宿舍位置，后面会让你在表单里补齐。'
      addMessage('assistant', `${locationSentence} 这个问题我先按“${profile.name}”来整理，请选择最接近的具体现象。`)
      return
    }

    if (phase.value === 'detail') {
      draft.value.issueDetail = text
      askImpact()
      return
    }

    if (phase.value === 'impact') {
      draft.value.impact = text
      askExtra()
      return
    }

    if (phase.value === 'extra') {
      draft.value.extra = draft.value.extra ? `${draft.value.extra}；${text}` : text
      await finalizeAssistantDraft()
      return
    }

    if (isCasualOnlyText(text)) {
      addMessage('assistant', '收到，我先保持当前摘要不变。需要补充或纠正时，直接告诉我新的报修内容就可以。')
      return
    }

    summary.value = summary.value ? `${summary.value}\n补充/纠正：${text}` : text
    form.value.description = summary.value
    addMessage('assistant', '已把这条补充加入摘要。你可以继续检查，或直接提交报修单。')
  }

  const sendInput = async () => {
    const text = input.value.trim()
    if (!text) return
    input.value = ''
    addMessage('student', text)
    await processFreeText(text)
  }

  const choose = async (choice: AssistantChoice) => {
    addMessage('student', choice.label)

    if (choice.action === 'starter') {
      await processFreeText(choice.value || choice.label)
      return
    }

    if (choice.action === 'detail') {
      await processFreeText(choice.value || choice.label)
      return
    }

    if (choice.action === 'impact') {
      await processFreeText(choice.value || choice.label)
      return
    }

    if (choice.action === 'extra') {
      if (choice.value) {
        await processFreeText(choice.value)
        return
      }
      phase.value = 'extra'
      choices.value = []
      addMessage('assistant', '好的，把要补充或纠正的内容发给我，我会合并到摘要里。')
      return
    }

    if (choice.action === 'finalize') {
      await finalizeAssistantDraft()
      return
    }

    reset()
  }

  const applySummaryToForm = () => {
    form.value.description = summary.value.trim()
    if (!form.value.categoryId) {
      form.value.categoryId = guessCategoryId()
    }
  }

  const reset = () => {
    fillLocationFromUser()
    phase.value = 'problem'
    messages.value = []
    choices.value = []
    input.value = ''
    summary.value = ''
    classificationError.value = ''
    classificationLoading.value = false
    chatError.value = ''
    chatLoading.value = false
    draft.value = {
      problem: '',
      issueType: 'other',
      issueDetail: '',
      impact: '',
      extra: ''
    }

    const user = getUser()
    const prefix = user?.realName ? `${user.realName}同学，` : ''
    const location = getCurrentUserLocation()
    const locationSentence = location
      ? `我已读取到你的宿舍信息：${location}。`
      : '我会先帮你整理问题，楼栋和房号可以稍后在表单里补齐。'
    choices.value = starterChoices.map(choice => ({ ...choice }))
    addMessage('assistant', `${prefix}${locationSentence}你可以直接描述问题，也可以从下面的常见报修开始。`)
  }

  return {
    phase,
    messages,
    choices,
    input,
    summary,
    inputPlaceholder,
    assistantSteps,
    activeQuestion,
    choiceHeading,
    progressPercent,
    collectedItems,
    locationText,
    classificationLoading,
    classificationError,
    chatLoading,
    chatError,
    sendInput,
    choose,
    applySummaryToForm,
    reset
  }
}
