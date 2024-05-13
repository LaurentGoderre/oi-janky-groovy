node {
	dir('cirros') {
		stage('Checkout') {
			git credentialsId: 'docker-library-bot', url: 'git@github.com:tianon/docker-brew-cirros.git'
		}

		stage('Download') {
			sh '''
				./download.sh
			'''
		}

		stage('Commit') {
			sh '''#!/usr/bin/env bash
				set -Eeuo pipefail
				set -x

				git config user.name 'Docker Library Bot'
				git config user.email 'github+dockerlibrarybot@infosiftr.com'

				version="$(< arches/version)"

				updated="$(curl -fsSL --header 'Accept: application/json' "https://github.com/cirros-dev/cirros/commits/$version.atom" | jq -r '.payload | first(.commitGroups[].commits[] | [ .committedDate, .authoredDate ] | sort | reverse[])' | xargs -i'{}' date --date '{}' --rfc-2822 --utc)"
				export GIT_AUTHOR_DATE="$updated"
				export GIT_COMMITTER_DATE="$GIT_AUTHOR_DATE"

				git add -A arches
				git commit -m "Add $version"
			'''
		}

		sshagent(credentials: ['docker-library-bot'], ignoreMissing: true) {
			stage('Push') {
				sh '''#!/usr/bin/env bash
					set -Eeuo pipefail
					set -x

					version="$(< arches/version)"

					git push -f origin HEAD:dist HEAD:"dist-${version%.*}" HEAD:"dist-$version"
				'''
			}
		}
	}
}
