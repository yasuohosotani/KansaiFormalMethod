model CriticalSections2 {
	transition system ts1 {
		states { nonCritical critical }
		events { request release }
		transitions {
			nonCritical * request -> critical
			critical * release -> nonCritical
		}
		initial states { nonCritical }
	}
	transition system ts2 {
		states { nonCritical critical }
		events { request release }
		transitions {
			nonCritical * request -> critical
			critical * release -> nonCritical
		}
		initial states { nonCritical }
	}
	transition system arbiter {
		states { unlock lock }
		events { request release }
		transitions {
			unlock * request -> lock
			lock * release -> unlock
		}
		initial states { unlock }
	}
	main = (ts1 ||| ts2) || arbiter
}

