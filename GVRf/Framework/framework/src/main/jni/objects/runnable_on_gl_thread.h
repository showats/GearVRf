#ifndef GL_PENDING_TASK_H_
#define GL_PENDING_TASK_H_

class RunnableOnGlThread {
public:
    virtual void runOnGlThread() = 0;
};

#endif // GL_PENDING_TASK_H_
