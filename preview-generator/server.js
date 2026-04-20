import Anthropic from "@anthropic-ai/sdk";
import express from "express";
import cors from "cors";

const app = express();
const port = 3001;

// Configure Claude client
const client = new Anthropic({
  apiKey: process.env.CLAUDE_API_KEY || "your-claude-api-key-here",
});

app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'preview-generator' });
});

// Preview generation endpoint
app.post('/generate-preview', async (req, res) => {
  try {
    const { systemPrompt, userPrompt, designStyle = 'modern' } = req.body;

    if (!systemPrompt || !userPrompt) {
      return res.status(400).json({ 
        error: 'Missing required fields: systemPrompt and userPrompt' 
      });
    }

    console.log(`Generating preview with design style: ${designStyle}`);
    
    const response = await client.messages.create({
      model: "claude-3-haiku-20240307",
      max_tokens: 4096,
      system: systemPrompt,
      messages: [
        { 
          role: "user", 
          content: userPrompt 
        }
      ],
    });

    const htmlContent = response.content[0].text;
    
    console.log(`Generated preview (${htmlContent.length} characters)`);
    
    res.json({
      success: true,
      htmlContent: htmlContent,
      designStyle: designStyle,
      generatedAt: new Date().toISOString(),
      model: "claude-3-haiku-20240307"
    });

  } catch (error) {
    console.error('Error generating preview:', error);
    res.status(500).json({
      success: false,
      error: error.message || 'Failed to generate preview'
    });
  }
});

app.listen(port, () => {
  console.log(`Preview generator service running on http://localhost:${port}`);
  console.log('Endpoints:');
  console.log('  GET  /health - Health check');
  console.log('  POST /generate-preview - Generate contract preview');
});