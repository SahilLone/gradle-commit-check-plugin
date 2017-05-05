package com.stefletcher.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer

class GitHookPlugin implements Plugin<Project> {
    protected commitMsgHook = '''#!/bin/sh

commitMessage=`cat $1`
a=`echo "$commitMessage" | grep 'EXPR_HERE'`
if [ $? -eq 0 ]; then
 exit 0
fi

echo "MESSAGE_HERE"
echo "COMMIT MESSAGE: $commitMessage"
exit 1
'''

    void apply(Project project) {

        project.extensions.create("gitCommitFormat", MessageRegExp)

        project.task('commitMessage') {

            def gitFolder = new File(project.projectDir.absolutePath + '/.git')

            doLast {
                def String expression = project.gitCommitFormat.expression
                if (expression == null || !gitFolder.exists() || project.gitCommitFormat.expression == '') {
                    // Do nothing...
                } else {
                    if (expression.charAt(expression.length() - 1) == "\$") {
                        expression = expression.substring(0, x.length() - 1)
                        expression = expression + "\\\$"
                    }
                    if(project.gitCommitFormat.template && project.gitCommitFormat.template.contains("\$commitMessage")){
                        project.gitCommitFormat.template = project.gitCommitFormat.template.replace("\$commitMessage", "COMMIT_MSG")
                    }
                    def msg = buildMessage(project.gitCommitFormat)

                    def hooks = new File(gitFolder.absolutePath + '/hooks')
                    def destination = new File(hooks.absolutePath + '/commit-msg')

                    def commitHook = commitMsgHook.replaceAll("EXPR_HERE", expression)
                    commitHook = commitHook.replace("MESSAGE_HERE", msg)

                    destination << commitHook.replaceAll("EXPR_HERE", expression)
                    destination.setExecutable(true)
                }
            }
        }

        project.test.finalizedBy project.commitMessage
    }

    protected String buildMessage(MessageRegExp messageRegExp){
        def expression = messageRegExp.expression
        def messageTemplate = messageRegExp.template
        if(messageTemplate && expression){
            return formatMessage(["expression":expression], messageTemplate)
        }else {
            return "Invalid commit message.  Must conform to the following pattern: ${expression}"
        }
    }

    protected String formatMessage(Map binding, String message){
        def engine = new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(message).make(binding)
        return template.toString().replace("COMMIT_MSG","\$commitMessage")
    }
}

class MessageRegExp {
    def expression = null;
    def template = 'Incorrect commit message format: <% print expression %>';
}
