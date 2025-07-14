package com.na982.opichelper.data.util

import com.na982.opichelper.domain.entity.Question
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.domain.entity.QuestionDifficulty

object SampleData {
    
    fun getSampleQuestions(): List<Question> {
        return listOf(
            Question(
                question = "Tell me about your hometown.",
                category = QuestionCategory.PERSONAL,
                difficulty = QuestionDifficulty.EASY,
                sampleAnswer = "I'm from Seoul, the capital city of South Korea. It's a vibrant and modern city with a population of over 10 million people. Seoul is known for its rich history, delicious food, and advanced technology. I love living here because there's always something new to discover, from traditional palaces to trendy neighborhoods like Gangnam and Hongdae."
            ),
            Question(
                question = "What do you do for a living?",
                category = QuestionCategory.WORK,
                difficulty = QuestionDifficulty.EASY,
                sampleAnswer = "I work as a software developer at a technology company. My main responsibilities include developing mobile applications and maintaining existing systems. I enjoy my job because it allows me to be creative and solve complex problems. The tech industry is constantly evolving, so I'm always learning new skills and technologies."
            ),
            Question(
                question = "Describe your educational background.",
                category = QuestionCategory.EDUCATION,
                difficulty = QuestionDifficulty.EASY,
                sampleAnswer = "I graduated from Seoul National University with a degree in Computer Science. During my studies, I focused on software engineering and artificial intelligence. I also participated in various research projects and internships, which helped me gain practical experience in the field."
            ),
            Question(
                question = "What are your hobbies and interests?",
                category = QuestionCategory.HOBBIES,
                difficulty = QuestionDifficulty.EASY,
                sampleAnswer = "I have several hobbies that I enjoy in my free time. I love reading books, especially science fiction and mystery novels. I also enjoy hiking and photography - I often go to nearby mountains on weekends to take pictures of nature. Additionally, I'm learning to play the guitar, which is both challenging and rewarding."
            ),
            Question(
                question = "Have you ever traveled abroad?",
                category = QuestionCategory.TRAVEL,
                difficulty = QuestionDifficulty.MEDIUM,
                sampleAnswer = "Yes, I've been fortunate to travel to several countries. Last year, I visited Japan and was amazed by the beautiful temples in Kyoto and the modern cityscape of Tokyo. I also traveled to Europe a few years ago, where I visited France, Italy, and Germany. Each country had its unique culture, food, and history that made the experience unforgettable."
            ),
            Question(
                question = "What do you think about the impact of technology on society?",
                category = QuestionCategory.TECHNOLOGY,
                difficulty = QuestionDifficulty.HARD,
                sampleAnswer = "Technology has had a profound impact on society, both positive and negative. On the positive side, it has improved communication, made information more accessible, and created new job opportunities. However, it has also led to concerns about privacy, social isolation, and job displacement. I believe we need to find a balance between embracing technological advances while addressing these challenges responsibly."
            ),
            Question(
                question = "How do you maintain a healthy lifestyle?",
                category = QuestionCategory.HEALTH,
                difficulty = QuestionDifficulty.MEDIUM,
                sampleAnswer = "I try to maintain a healthy lifestyle through regular exercise and balanced nutrition. I go to the gym three times a week for strength training and cardio. I also make sure to eat plenty of vegetables and lean proteins while limiting processed foods. Additionally, I prioritize getting enough sleep and managing stress through meditation and outdoor activities."
            ),
            Question(
                question = "What are your thoughts on environmental protection?",
                category = QuestionCategory.ENVIRONMENT,
                difficulty = QuestionDifficulty.HARD,
                sampleAnswer = "Environmental protection is crucial for the future of our planet. I believe everyone has a responsibility to reduce their carbon footprint through actions like recycling, using public transportation, and supporting renewable energy. Governments and businesses should also implement policies that promote sustainability. Climate change is a global challenge that requires collective action from individuals, organizations, and nations."
            ),
            Question(
                question = "How do you handle stress and pressure?",
                category = QuestionCategory.PERSONAL,
                difficulty = QuestionDifficulty.MEDIUM,
                sampleAnswer = "When I'm under stress, I try to identify the source and break down problems into smaller, manageable tasks. I find that exercise helps me clear my mind, so I often go for a run or do yoga. I also practice time management to avoid feeling overwhelmed. Talking to friends and family provides emotional support, and sometimes I just need to take a short break to recharge."
            ),
            Question(
                question = "What are your career goals for the future?",
                category = QuestionCategory.WORK,
                difficulty = QuestionDifficulty.MEDIUM,
                sampleAnswer = "My short-term goal is to become a senior developer and lead small projects. In the long term, I'd like to move into a management role or start my own technology company. I'm also interested in specializing in artificial intelligence and machine learning. I believe continuous learning and adapting to new technologies will be key to achieving these goals."
            )
        )
    }
} 