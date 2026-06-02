export const navItems = [
  { key: 'overview', label: '数据总览', icon: 'DataLine' },
  { key: 'video', label: '视频分析', icon: 'VideoCamera' },
  { key: 'danmaku', label: '弹幕分析', icon: 'Histogram' },
  { key: 'comment', label: '评论洞察', icon: 'ChatDotRound' },
  { key: 'creator', label: 'UP主画像', icon: 'User' },
  { key: 'task', label: '任务中心', icon: 'Tickets' },
]

export const recommendations = [
  {
    title: '补齐高峰片段复盘',
    level: '高优先级',
    type: 'warning',
    text: '优先查看弹幕高峰点对应的视频片段，判断它是剧情高潮、争议点还是互动梗。',
  },
  {
    title: '建立负面评论处理流',
    level: '必要',
    type: 'danger',
    text: '当负面评论集中在同一视频或同一关键词时，建议加入置顶解释、二次剪辑或运营回复。',
  },
  {
    title: '沉淀爆款视频特征',
    level: '增长',
    type: 'success',
    text: '把热度高、互动率高、正向情感高的视频归入样本池，用于后续选题和标题复盘。',
  },
  {
    title: '按UP主拆解能力短板',
    level: '分析',
    type: 'primary',
    text: '用雷达图比较平均播放、热度、情感和点赞，避免只按播放量评价创作者。',
  },
]
