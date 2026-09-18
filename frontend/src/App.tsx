import { useQuery } from '@tanstack/react-query'
import { ping } from './api/health'

function App() {
  const { data, isPending, error } = useQuery({ queryKey: ['ping'], queryFn: ping })

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="rounded-xl bg-white p-8 shadow">
        <h1 className="text-2xl font-bold text-slate-800">Smart Trip Planner</h1>
        {isPending && <p className="mt-4 text-slate-500">Đang gọi /api/v1/ping...</p>}
        {error && (
          <p className="mt-4 text-red-600">Không gọi được backend: {error.message}</p>
        )}
        {data && (
          <p className="mt-4 text-green-600">
            Backend trả về: <code className="font-mono">{data.data}</code>
          </p>
        )}
      </div>
    </main>
  )
}

export default App