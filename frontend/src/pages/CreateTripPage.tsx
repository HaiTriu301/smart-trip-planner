import { CreateTripWizard } from '../features/trips/CreateTripWizard'

export function CreateTripPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-[32px] leading-10 font-bold tracking-[-0.02em] text-ink">Tạo chuyến đi</h1>
        <p className="mt-1 text-gray-600">Ba bước: thông tin chung, điểm đến và ngày đi.</p>
      </div>
      <div className="rounded-card border border-tide bg-white p-6 sm:p-8">
        <CreateTripWizard />
      </div>
    </div>
  )
}
