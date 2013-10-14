model CriticalSections1 {
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
	main = ts1 ||| ts2
}
