<template>
  <div class="ai-chat">
    <div class="messages">
      <div v-for="msg in messages" :key="msg.id" class="message">
        {{ msg.text }}
      </div>
    </div>
    <input v-model="input" @keyup.enter="sendMessage" placeholder="Ask AI..." />
  </div>
</template>

<script>
export default {
  name: 'AiChatBox',
  data() {
    return {
      messages: [],
      input: ''
    };
  },
  methods: {
    sendMessage() {
      // 发送消息到AI
      this.messages.push({ id: Date.now(), text: this.input });
      this.input = '';
    }
  }
}
</script>
