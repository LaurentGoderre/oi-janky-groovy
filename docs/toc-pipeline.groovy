properties([
	buildDiscarder(logRotator(daysToKeepStr: '14')),
	disableConcurrentBuilds(),
	pipelineTriggers([
		cron('H H * * *'),
	]),
])

node {
	stage('Checkout') {
		checkout(
			poll: true,
			scm: [
				$class: 'GitSCM',
				userRemoteConfigs: [[
					url: 'git@github.com:docker-library/official-images.git',
					credentialsId: 'docker-library-bot',
					name: 'origin',
				]],
				branches: [[name: '*/master']],
				extensions: [
					[
						$class: 'RelativeTargetDirectory',
						relativeTargetDir: 'oi',
					],
					[
						$class: 'CleanCheckout',
					],
					[
						$class: 'PathRestriction',
						excludedRegions: '',
						includedRegions: [
							'README.md',
							'toc.sh',
						].join('\n'),
					],
				],
				doGenerateSubmoduleConfigurations: false,
				submoduleCfg: [],
			],
		)
		checkout(
			poll: true,
			scm: [
				$class: 'GitSCM',
				userRemoteConfigs: [[
					url: 'git@github.com:docker-library/docs.git',
					credentialsId: 'docker-library-bot',
					name: 'origin',
				]],
				branches: [[name: '*/master']],
				extensions: [
					[
						$class: 'CleanCheckout',
					],
					[
						$class: 'RelativeTargetDirectory',
						relativeTargetDir: 'd',
					],
					[
						$class: 'PathRestriction',
						excludedRegions: '',
						includedRegions: [
							'README.md',
						].join('\n'),
					],
				],
				doGenerateSubmoduleConfigurations: false,
				submoduleCfg: [],
			],
		)
		checkout(
			poll: true,
			scm: [
				$class: 'GitSCM',
				userRemoteConfigs: [[
					url: 'git@github.com:docker-library/faq.git',
					credentialsId: 'docker-library-bot',
					name: 'origin',
				]],
				branches: [[name: '*/master']],
				extensions: [
					[
						$class: 'CleanCheckout',
					],
					[
						$class: 'RelativeTargetDirectory',
						relativeTargetDir: 'faq',
					],
					[
						$class: 'PathRestriction',
						excludedRegions: '',
						includedRegions: [
							'README.md',
						].join('\n'),
					],
				],
				doGenerateSubmoduleConfigurations: false,
				submoduleCfg: [],
			],
		)
	}

	ansiColor('xterm') {
		stage('official-images TOC') {
			sh '''
				oi/toc.sh oi/README.md
			'''
		}
		stage('docs TOC') {
			sh '''
				oi/toc.sh d/README.md
			'''
		}
		stage('faq TOC') {
			sh '''
				oi/toc.sh faq/README.md
			'''
		}

		stage('Commit') {
			sh('''
				for dir in oi d faq; do
					git -C "$dir" config user.name 'Docker Library Bot'
					git -C "$dir" config user.email 'doi+docker-library-bot@docker.com'

					git -C "$dir" add README.md || :
					git -C "$dir" commit -m 'Update Table of Contents' || :
				done
			''')
		}

		sshagent(credentials: ['docker-library-bot'], ignoreMissing: true) {
			stage('Push') {
				sh '''
					for dir in oi d faq; do
						git -C "$dir" push origin HEAD:refs/heads/master
					done
				'''
			}
		}
	}
}
